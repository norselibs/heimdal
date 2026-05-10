/**
 * hm-form — Heimdal's core framework component.
 *
 * Reads the JSON form definition from its child <script type="application/json">,
 * builds the component tree, wires events, and handles the validate/submit
 * protocol defined in docs/decisions/2026-05-10_wire-protocol-v0.md.
 */
class HmForm extends HTMLElement {
    #schema  = null;
    #seq     = 0;
    #fields  = new Map(); // fieldName → field element

    connectedCallback() {
        const scriptEl = this.querySelector('script[type="application/json"]');
        if (!scriptEl) return;
        this.#schema = JSON.parse(scriptEl.textContent);
        scriptEl.remove();
        this.#render();
    }

    #render() {
        const form = document.createElement('form');
        form.noValidate = true;

        for (const item of this.#schema.items) {
            if ('section' in item) {
                form.append(this.#createSection(item));
            } else if (item.name != null) {
                form.append(this.#createField(item));
            } else {
                // Layout component — no name, no value, no validation wiring
                form.append(this.#createLayout(item));
            }
        }

        const actions = document.createElement('div');
        actions.className = 'hm-actions';
        for (const action of this.#schema.actions ?? []) {
            if (action.type === 'submit') {
                const btn = document.createElement('button');
                btn.type = 'submit';
                btn.textContent = action.label;
                actions.append(btn);
            }
        }
        form.append(actions);
        form.addEventListener('submit', e => { e.preventDefault(); this.#submit(); });

        this.append(form);
        this.#evaluateSections();
    }

    #createSection(sectionDef) {
        const wrapper = document.createElement('div');
        wrapper.dataset.hmSection    = sectionDef.section;
        wrapper.dataset.hmVisibleWhen = JSON.stringify(sectionDef.visibleWhen);

        for (const fieldDef of sectionDef.items) {
            wrapper.append(this.#createField(fieldDef));
        }
        return wrapper;
    }

    // STANDARD_KEYS are handled explicitly; everything else is passed through
    // as a JS property so components can receive arbitrary extra config
    // (e.g. options for selects, currency for money fields).
    static #STANDARD_KEYS = new Set(['component', 'name', 'label', 'value', 'required', 'validateOn', 'validates']);

    #createField(def) {
        const el = document.createElement(def.component);

        if (def.name  != null) el.setAttribute('name',  def.name);
        if (def.label != null) el.setAttribute('label', def.label);
        if (def.value != null) el.setAttribute('value', def.value);
        if (def.required)      el.setAttribute('required', '');

        for (const [key, val] of Object.entries(def)) {
            if (!HmForm.#STANDARD_KEYS.has(key)) el[key] = val;
        }

        this.#fields.set(def.name, el);

        if (def.validateOn === 'blur') {
            el.addEventListener('blur', () => this.#validate(def.name), true);
        } else if (def.validateOn === 'change') {
            el.addEventListener('change', () => this.#validate(def.name));
        }

        el.addEventListener('change', () => this.#evaluateSections());
        el.addEventListener('input',  () => this.#evaluateSections());

        return el;
    }

    #evaluateSections() {
        const values = this.#collectValues();
        for (const section of this.querySelectorAll('[data-hm-section]')) {
            const pred = JSON.parse(section.dataset.hmVisibleWhen);
            section.hidden = !this.#evaluate(pred, values);
        }
    }

    #evaluate(pred, values) {
        const v = values[pred.field] ?? '';
        switch (pred.op) {
            case 'eq':  return v === String(pred.value);
            case 'neq': return v !== String(pred.value);
            case 'in':  return pred.values.map(String).includes(v);
            default:    return true;
        }
    }

    #collectValues() {
        const result = {};
        for (const [name, el] of this.#fields) {
            result[name] = el.value ?? '';
        }
        return result;
    }

    async #validate(fieldName) {
        const seq = ++this.#seq;
        let result;
        try {
            const res = await fetch(this.#schema.eventEndpoint, {
                method:  'POST',
                headers: { 'Content-Type': 'application/json' },
                body:    JSON.stringify({ type: 'validate', field: fieldName, seq, values: this.#collectValues() })
            });
            result = await res.json();
        } catch {
            return; // network failure — leave existing error state unchanged
        }
        if (result.seq < this.#seq) return; // stale response
        this.#applyErrors(result.errors ?? {});
    }

    async #submit() {
        const seq = ++this.#seq;
        let res;
        try {
            res = await fetch(this.#schema.submitEndpoint, {
                method:  'POST',
                headers: { 'Content-Type': 'application/json' },
                body:    JSON.stringify({ seq, values: this.#collectValues() })
            });
        } catch {
            return;
        }
        if (res.status === 422) {
            this.#applyErrors((await res.json()).errors ?? {});
        } else if (res.ok) {
            const body = await res.json();
            if (body.redirect) window.location.assign(body.redirect);
        }
    }

    #createLayout(def) {
        const el = document.createElement(def.component);
        for (const [key, val] of Object.entries(def)) {
            if (key !== 'component') el[key] = val;
        }
        return el;
    }

    #applyErrors(errors) {
        for (const [name, messages] of Object.entries(errors)) {
            this.#fields.get(name)?.setErrors?.(messages);
        }
    }
}

customElements.define('hm-form', HmForm);
