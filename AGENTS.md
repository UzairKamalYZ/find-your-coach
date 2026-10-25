# Architecture rules
- Keep account functionality explicitly demo-only and local to the frontend; no provider connection or backend is implied by demo sign-ins.
- Share account types and demo identity normalization in a standalone module so account forms and persisted profiles use the same shape.
- Keep reusable contact form fields separate from account pages so labels and input behavior stay consistent.