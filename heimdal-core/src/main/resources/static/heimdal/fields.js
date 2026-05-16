/**
 * Built-in Heimdal field components.
 *
 * These are reference implementations — a project's frontend team can replace
 * or extend any of them. The contract with hm-form is minimal:
 *
 *   value        getter   current field value as a string
 *   setErrors(messages: string[])   display or clear inline errors
 *
 * Components receive their configuration via standard HTML attributes (name,
 * label, value, required) plus any extra JS properties set by hm-form for
 * component-specific config (e.g. options for selects, currency for money).
 */

// ---------------------------------------------------------------------------
// Shared base — handles the error slot and attribute-change re-rendering
// ---------------------------------------------------------------------------

class HmBaseField extends HTMLElement {
    static observedAttributes = ['name', 'label', 'value', 'required', 'readonly'];

    connectedCallback()                          { this._render(); }
    attributeChangedCallback()                   { if (this.isConnected) this._render(); }
    setErrors(messages)                          { this._setErrors(messages); }

    _setErrors(messages) {
        const el = this.querySelector('.hm-error');
        if (!el) return;
        el.textContent = messages[0] ?? '';
        el.hidden = messages.length === 0;
    }

    _attr(name, fallback = '') {
        return this.getAttribute(name) ?? fallback;
    }

    _readonly() {
        return this.hasAttribute('readonly');
    }

    _errorSlot() {
        return '<span class="hm-error" hidden></span>';
    }
}

// ---------------------------------------------------------------------------
// hm-text-field
// ---------------------------------------------------------------------------

class HmTextField extends HmBaseField {
    static heimdal = { type: 'string', default: true };
    get value() { return this.querySelector('input')?.value ?? this._attr('value'); }

    _render() {
        const name     = this._attr('name');
        const label    = this._attr('label');
        const val      = this._attr('value');
        const required = this.hasAttribute('required');
        this.innerHTML = `
            <label class="hm-field">
                <span class="hm-label">${label}${required ? ' <span aria-hidden="true">*</span>' : ''}</span>
                <input type="text" name="${name}" value="${val}"${required ? ' required' : ''}${this._readonly() ? ' readonly' : ''}>
                ${this._errorSlot()}
            </label>`;
    }
}
customElements.define('hm-text-field', HmTextField);

// ---------------------------------------------------------------------------
// hm-textarea-field
// ---------------------------------------------------------------------------

class HmTextareaField extends HmBaseField {
    static heimdal = { type: 'string', multiline: true, default: true };
    get value() { return this.querySelector('textarea')?.value ?? this._attr('value'); }

    _render() {
        const name     = this._attr('name');
        const label    = this._attr('label');
        const val      = this._attr('value');
        const required = this.hasAttribute('required');
        this.innerHTML = `
            <label class="hm-field">
                <span class="hm-label">${label}${required ? ' <span aria-hidden="true">*</span>' : ''}</span>
                <textarea name="${name}"${required ? ' required' : ''}${this._readonly() ? ' readonly' : ''}>${val}</textarea>
                ${this._errorSlot()}
            </label>`;
    }
}
customElements.define('hm-textarea-field', HmTextareaField);

// ---------------------------------------------------------------------------
// hm-number-field
// ---------------------------------------------------------------------------

class HmNumberField extends HmBaseField {
    static heimdal = { types: ['integer', 'long', 'decimal'], default: true };
    get value() { return this.querySelector('input')?.value ?? this._attr('value'); }

    _render() {
        const name     = this._attr('name');
        const label    = this._attr('label');
        const val      = this._attr('value');
        const required = this.hasAttribute('required');
        this.innerHTML = `
            <label class="hm-field">
                <span class="hm-label">${label}${required ? ' <span aria-hidden="true">*</span>' : ''}</span>
                <input type="number" name="${name}" value="${val}"${required ? ' required' : ''}${this._readonly() ? ' readonly' : ''}>
                ${this._errorSlot()}
            </label>`;
    }
}
customElements.define('hm-number-field', HmNumberField);

// ---------------------------------------------------------------------------
// hm-checkbox-field
// ---------------------------------------------------------------------------

class HmCheckboxField extends HmBaseField {
    static heimdal = { type: 'boolean', default: true };
    get value() {
        return String(this.querySelector('input')?.checked ?? this._attr('value') === 'true');
    }

    _render() {
        const name    = this._attr('name');
        const label   = this._attr('label');
        const checked = this._attr('value') === 'true';
        this.innerHTML = `
            <label class="hm-field hm-field--checkbox">
                <input type="checkbox" name="${name}"${checked ? ' checked' : ''}${this._readonly() ? ' disabled' : ''}>
                <span class="hm-label">${label}</span>
                ${this._errorSlot()}
            </label>`;
    }
}
customElements.define('hm-checkbox-field', HmCheckboxField);

// ---------------------------------------------------------------------------
// hm-date-field
// ---------------------------------------------------------------------------

class HmDateField extends HmBaseField {
    static heimdal = { type: 'date', default: true };
    get value() { return this.querySelector('input')?.value ?? this._attr('value'); }

    _render() {
        const name     = this._attr('name');
        const label    = this._attr('label');
        const val      = this._attr('value');
        const required = this.hasAttribute('required');
        this.innerHTML = `
            <label class="hm-field">
                <span class="hm-label">${label}${required ? ' <span aria-hidden="true">*</span>' : ''}</span>
                <input type="date" name="${name}" value="${val}"${required ? ' required' : ''}${this._readonly() ? ' readonly' : ''}>
                ${this._errorSlot()}
            </label>`;
    }
}
customElements.define('hm-date-field', HmDateField);

// ---------------------------------------------------------------------------
// hm-select-field
//
// options is set as a JS property (not an attribute) because it is an array.
// hm-form sets el.options = [...] after createElement, before insertion.
// ---------------------------------------------------------------------------

class HmSelectField extends HmBaseField {
    // options is not in observedAttributes — it is set as a property
    set options(value) {
        this._options = value;
        if (this.isConnected) this._render();
    }

    get value() { return this.querySelector('select')?.value ?? this._attr('value'); }

    _render() {
        const name     = this._attr('name');
        const label    = this._attr('label');
        const current  = this._attr('value');
        const required = this.hasAttribute('required');
        const options  = this._options ?? [];

        const optHtml = options
            .map(o => `<option value="${o.value}"${o.value === current ? ' selected' : ''}>${o.label}</option>`)
            .join('');

        this.innerHTML = `
            <label class="hm-field">
                <span class="hm-label">${label}${required ? ' <span aria-hidden="true">*</span>' : ''}</span>
                <select name="${name}"${required ? ' required' : ''}>
                    <option value="">— select —</option>
                    ${optHtml}
                </select>
                ${this._errorSlot()}
            </label>`;
    }
}
customElements.define('hm-select-field', HmSelectField);

// ---------------------------------------------------------------------------
// hm-collection-field
//
// Generic inline editable list. Receives a `columns` JS property (array of
// {name, label, type, required?}) from hm-form and renders an editable table.
// get value() returns a JavaScript array so JSON.stringify in hm-form nests
// it correctly as a JSON array in the submitted body.
// ---------------------------------------------------------------------------

class HmCollectionField extends HTMLElement {
    #columns = [];
    #rows    = [];

    set columns(value) {
        this.#columns = value ?? [];
        if (this.isConnected) this.#render();
    }

    get value() {
        return this.#collectRows();   // returns a JS array, not a string
    }

    setErrors(messages) {
        const el = this.querySelector('.hm-error');
        if (!el) return;
        el.textContent = messages[0] ?? '';
        el.hidden = messages.length === 0;
    }

    connectedCallback() {
        const raw = this.getAttribute('value') ?? '[]';
        try { this.#rows = JSON.parse(raw); } catch { this.#rows = []; }
        if (!Array.isArray(this.#rows)) this.#rows = [];
        this.#render();
    }

    #render() {
        const label    = this.getAttribute('label') ?? '';
        const required = this.hasAttribute('required');
        const cols     = this.#columns;

        const headers = cols.map(c => `<th>${c.label}</th>`).join('') + '<th></th>';
        const bodyRows = this.#rows.map((row, i) => this.#rowHtml(row, i, cols)).join('');

        this.innerHTML = `
            <div class="hm-collection">
                <span class="hm-label">${label}${required ? ' <span aria-hidden="true">*</span>' : ''}</span>
                <table class="hm-collection-table">
                    <thead><tr>${headers}</tr></thead>
                    <tbody>${bodyRows}</tbody>
                </table>
                <button type="button" class="hm-collection-add">+ Add row</button>
                <span class="hm-error" hidden></span>
            </div>`;

        this.querySelector('.hm-collection-add')
            .addEventListener('click', () => { this.#rows = this.#collectRows(); this.#rows.push({}); this.#render(); });

        this.querySelectorAll('.hm-collection-remove').forEach((btn, i) =>
            btn.addEventListener('click', () => { this.#rows = this.#collectRows(); this.#rows.splice(i, 1); this.#render(); }));
    }

    #rowHtml(row, i, cols) {
        const cells = cols.map(col => {
            const val  = row[col.name] ?? '';
            const type = col.type === 'integer' || col.type === 'decimal' ? 'number'
                       : col.type === 'date' ? 'date' : 'text';
            return `<td><input type="${type}" data-col="${col.name}" value="${String(val).replace(/"/g, '&quot;')}"></td>`;
        }).join('');
        return `<tr>${cells}<td><button type="button" class="hm-collection-remove" title="Remove">✕</button></td></tr>`;
    }

    #collectRows() {
        const rows = [];
        this.querySelectorAll('tbody tr').forEach(tr => {
            const row = {};
            tr.querySelectorAll('input[data-col]').forEach(inp => { row[inp.dataset.col] = inp.value; });
            rows.push(row);
        });
        return rows;
    }
}
customElements.define('hm-collection-field', HmCollectionField);
