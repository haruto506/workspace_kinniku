# Repository agent instructions

## Detailed design review

- When a task changes `docs/detailed_design.md`, delegate a final review to the project custom agent named `detailed_design_reviewer` before completing the task.
- Wait for that agent to finish and include its overall verdict in the task summary.
- The reviewer owns `docs/detailed_design_review.md`; do not edit that file in parallel with it.
- A review-only request for `docs/detailed_design.md` should also use `detailed_design_reviewer`.
