# heimdal-react

A React frontend for Heimdal forms and lists. Demonstrates that Heimdal is a
**wire protocol** — the Java backend and web-component frontend are replaceable.
This app talks to the same Java server as the standard web-component frontend
and renders identical forms using React components.

## How it works

The Java backend already serves form and list schemas as JSON embedded in HTML.
This app adds one thing: when the client sends `Accept: application/json`, the
server returns the schema directly — no HTML wrapper.

```
GET /bikes/new   Accept: application/json
→  { formId, eventEndpoint, items, actions, … }

GET /bikes       Accept: application/json
→  { listId, columns, rows, actions, … }

POST /bikes/new  { type: "validate", field, seq, values }
→  { seq, errors }                       ← same wire protocol

POST /bikes/save { name, bikeType, … }
→  { redirect: "/bikes" }               ← same wire protocol
```

## Running

Start the Java backend first:
```bash
# from the repo root
./gradlew :heimdal-integration-test:run
```

Then in a separate terminal:
```bash
cd heimdal-react
npm install
npm run dev
```

Open `http://localhost:5173/bikes` — the Vite proxy forwards API calls to the
Java server at `localhost:8080`.

## Custom components

Register React components for custom component names before rendering:

```jsx
import { registerField } from './fields.jsx';
import RatingField from './RatingField.jsx';

registerField('hm-rating-field', RatingField);
```

The component receives `{ def, value, errors, onChange, onBlur }` props.
`def` is the field definition from the schema (includes any `extraJson` props).
