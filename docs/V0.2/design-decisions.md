# UI design decisions

The first UI/UX iteration keeps only the elements needed to validate the provider contract without overloading the screen.

Visible components:

- application header with provider status and last action status;
- one dominant current-value card;
- one compact condition badge;
- six glance cards for the most useful secondary values;
- one metric selector;
- one interval input;
- one immediate-read action;
- one subscribe action;
- one active-stream table;
- one reference tab for supported metrics and provider limits.

The layout separates operational tasks from reference information. The `Live` tab supports the daily workflow: select, read, subscribe and stop. The `Reference` tab keeps technical metadata available without making it permanently visible.

The visual language is intentionally flat. Borders, rounded corners and saturated accent colors provide hierarchy without shadows, gradients, blur or heavy motion.

The palette uses dark surfaces and bright foreground values so that the interface remains readable while preserving a distinctive identity. Accent colors are not used as the only meaning carrier; each status or action also has text.
