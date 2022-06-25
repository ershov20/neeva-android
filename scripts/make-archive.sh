#!/bin/bash

if [ $# -lt 2 ]; then
    echo "$(basename $0) [--fast] apk-path archive-path"
    exit 1
fi

scripts_dir=$(dirname $0)
build_tools=$($scripts_dir/get-build-tools-path.sh || exit 1)

fast_mode=0
if [ "$1" = "--fast" ]; then
    fast_mode=1
    shift
fi

apk=$1
apk_name=$(basename $apk)
archive=$2
archive_name=$(basename $archive)
aapt2="$build_tools/aapt2"

if [ ! -f $apk ]; then
    echo "ERROR: $apk not found!"
    exit 1
fi

if [ -e $archive ]; then
    echo "ERROR: $archive already exists!"
    exit 1
fi

echo "Making archive \"$archive_name.zip\" from $apk_name..."

files_total=
files_processed=0

copy_from_zip_to_zip() {
    src_zip=$1
    src_file=$2
    dest_zip=$3
    dest_dir=$4
    tmp_dir=$5/cpz2z

    mkdir -p $tmp_dir/$dest_dir

    unzip $src_zip $src_file -d $tmp_dir/$dest_dir > /dev/null

    dest_zip_full_path="$(pwd)/$dest_zip"

    (cd $tmp_dir && zip -r $dest_zip_full_path $dest_dir > /dev/null)

    rm -fr $tmp_dir

    files_processed=$(expr $files_processed + 1)
    line="[ $files_processed of $files_total processed ]"
    echo -ne "\r$(echo $line)"
}

# NOTE: Cannot simply unzip the APK and then rearrange its contents since there
# may be multiple file names that collide on a case-insensitive file system.
# Instead, this script takes care to copy files one by one from the source APK
# into the destination archive ZIP.

tmpdir=$(mktemp -d -t $(basename $0)) || exit 1

echo "Converting $(basename $apk) from binary XML to protobuf..."
converted_apk="$tmpdir/$apk_name-converted"
$aapt2 convert -o $converted_apk --output-format proto $apk || exit 1

file_list="$tmpdir/files.txt"
zipinfo -1 $converted_apk > $file_list

files_total=$(cat $file_list | wc -l)

echo "Building archive..."

if [ $fast_mode = 1 ]; then
    unzipped_apk_dir="$converted_apk.unzipped"

    echo -n "[ Fast mode ]"

    mkdir -p $unzipped_apk_dir/root
    unzip $converted_apk -d $unzipped_apk_dir/root > /dev/null

    archive_zip_full_path="$(pwd)/$archive.zip"

    mv $unzipped_apk_dir/root/assets $unzipped_apk_dir
    mv $unzipped_apk_dir/root/lib $unzipped_apk_dir
    mv $unzipped_apk_dir/root/res $unzipped_apk_dir

    mkdir $unzipped_apk_dir/dex
    mv $unzipped_apk_dir/root/*dex $unzipped_apk_dir/dex

    mkdir $unzipped_apk_dir/manifest
    mv $unzipped_apk_dir/root/AndroidManifest.xml $unzipped_apk_dir/manifest

    mv $unzipped_apk_dir/root/resources.pb $unzipped_apk_dir

    (cd $unzipped_apk_dir && zip -r $archive_zip_full_path . > /dev/null)
else
    dex_files=$(cat $file_list | egrep '\.dex$')
    res_files=$(cat $file_list | egrep '^res/')
    lib_files=$(cat $file_list | egrep '^lib/')
    asset_files=$(cat $file_list | egrep '^assets/')
    other_files=$(cat $file_list | egrep -v '\.dex$|^res/|^lib/|^assets/|AndroidManifest.xml|resources.pb')

    copy_from_zip_to_zip $converted_apk resources.pb $archive.zip . $tmpdir
    copy_from_zip_to_zip $converted_apk AndroidManifest.xml $archive.zip manifest $tmpdir

    for file in $res_files; do
        copy_from_zip_to_zip $converted_apk $file $archive.zip . $tmpdir
    done

    for file in $lib_files; do
        copy_from_zip_to_zip $converted_apk $file $archive.zip . $tmpdir
    done

    for file in $asset_files; do
        copy_from_zip_to_zip $converted_apk $file $archive.zip . $tmpdir
    done

    for file in $dex_files; do
        copy_from_zip_to_zip $converted_apk $file $archive.zip dex $tmpdir
    done

    for file in $other_files; do
        copy_from_zip_to_zip $converted_apk $file $archive.zip root $tmpdir
    done
fi

echo ""
