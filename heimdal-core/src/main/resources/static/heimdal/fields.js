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
    static observedAttributes = ['name', 'label', 'value', 'required'];

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
                <input type="text" name="${name}" value="${val}"${required ? ' required' : ''}>
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
                <textarea name="${name}"${required ? ' required' : ''}>${val}</textarea>
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
                <input type="number" name="${name}" value="${val}"${required ? ' required' : ''}>
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
                <input type="checkbox" name="${name}"${checked ? ' checked' : ''}>
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
                <input type="date" name="${name}" value="${val}"${required ? ' required' : ''}>
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
