# PureCast does not currently use reflection-based application APIs.
# Keep rules should be added only when a concrete release-mode failure requires them.

# Keep a reviewable audit trail of what R8 removes and what it retains.
-printusage build/outputs/mapping/release/usage.txt
-printseeds build/outputs/mapping/release/seeds.txt
