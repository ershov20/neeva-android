NOTE: the base__R.jar file in this directory provides a no-op implementation of
org.chromium.base.R with just a single onResourcesLoaded method. This is to
satisfy Chromium, which calls this non-standard method on R to perform fixup of
resource identifiers. That fixup is not needed for us given the way we package
resources using gradle. However, we still need to provide that function.
