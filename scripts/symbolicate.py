#!python3

# This is a simple script that loads in a symbols file and then reads undecoded
# stack frames from stdin. Upon receiving an empty line, all stack frames
# entered up to that point will be decoded and printed out. This way you can
# either just enter a single address or copy/paste an undecoded crash dump and
# see the corresponding symbols.

import gzip
import sys

if len(sys.argv) == 1:
    print("Usage: symbolicate.py path-to-symbols-file")
    exit(1)

symbols_file = sys.argv[1]

def is_all_spaces(bytes):
    for b in bytes:
        if b != 0x20:
            return False
    return True

class Symbol:
    def __init__(self, address, size, name):
        self.address = address
        self.size = size
        self.name = name

symbols = []

def lookup_symbol(pc):
    # Search for symbol that contains the given address. Brute force for now;
    # could binary search to speed up.
    for symbol in symbols:
        if symbol.address < pc and pc < symbol.address + symbol.size:
            return symbol
    return None

def print_decoded_frame(frame):
    # Support input like: "  #00  pc 000000000address  path"
    components = frame.split(' ')
    # Take first component that looks like an address, a hex number.
    pc = 0
    for c in components:
        try:
            pc = int(c, 16)
        except:
            continue
    # Search for corresponding symbol.
    symbol = lookup_symbol(pc)
    if symbol != None:
        print(symbol.name)
    else:
        print("### Unknown ###")

class Stack:
    frames=[]
    def add_frame(self, frame):
        self.frames.append(frame)
    def dump_decoded(self):
        for frame in self.frames:
            print_decoded_frame(frame)
        self.frames = []

print("Loading symbols file...")

with gzip.open(symbols_file, 'rb') as f:
    did_first = False
    for line in f:
        if not did_first:  # Skip first line
            did_first = True
            continue
        offset = 0

        # Example file contents:
        # b'             VMA              LMA     Size Align Out     In      Symbol\n'
        # b'         26bf640          26bf640  458021c    64 .text\n'
        # b'         26bf640          26bf640       64     4         ../../third_party/android_ndk/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/aarch64-linux-android/23/crtbegin_so.o:(.text)\n'
        # b'         26bf640          26bf640        0     1                 $x.1\n'
        # b'         26bf640          26bf640       10     1                 __on_dlclose\n'
        # b'         26bf658          26bf658        8     1                 __on_dlclose_late\n'
        # b'         26bf660          26bf660       14     1                 __atexit_handler_wrapper\n'
        # b'         26bf674          26bf674       20     1                 atexit\n'
        # b'         26bf694          26bf694       10     1                 pthread_atfork\n'
        #
        # Looks like the following sequence:
        #   [ VMA char*16 ] SPACE
        #   [ LMA char*16 ] SPACE
        #   [ Size char*8 ] SPACE
        #   [ Align char*5 ] SPACE
        #   [ Out-prefix char*8 ]
        #   [ In-prefix char*8 ]
        #   [ Symbol char*N ]
        #
        # VMA / LMA appear to be identical and indicate the offset of the data.
        # Size is the length of the data
        # Align is the alignment of the data
        #
        # The string at the end of a line is either interpreted as the name of
        # the output section, the name of the input file or the name of the
        # symbol (what we actually care about).
        #
        # The tab offset of the string at the end of a line indicates how it
        # should be interpreted. If out-prefix is not empty, then the string
        # indicates a section name. If out-prefix is empty but in-prefix is
        # not empty, then the string indicates an input file. Else the string
        # indicates a symbol name.

        vma = line[offset:offset+16]
        offset += 16 + 1
        lma = line[offset:offset+16]
        offset += 16 + 1
        size = line[offset:offset+8]
        offset += 8 + 1
        align = line[offset:offset+5]
        offset += 5 + 1

        remainder = line[offset:]

        section = ""
        file = ""
        symbol = ""
        if not is_all_spaces(remainder[:8]):
            section = remainder[:-1]
        else:
            remainder = remainder[8:]
            if not is_all_spaces(remainder[:8]):
                file = remainder[:-1]
            else:
                symbol = remainder[8:-1]

        if symbol != "":
            symbols.append(Symbol(int(vma, 16), int(size, 16), symbol.decode("utf-8")))

print("Ready")

stack = Stack()
while True:
    print("Enter stack trace to decode:")
    while line := input():
        stack.add_frame(line)
    stack.dump_decoded()
