# Add project-specific R8 rules here.
# Room (annotation-processor generated code, not reflection) and Compose
# both ship their own consumer rules inside their AARs, so nothing extra
# is needed for them by default. Add rules here if a release build turns
# up a real ClassNotFoundException/NoSuchMethodError that a debug build
# doesn't -- don't add speculative keep rules before that happens.
