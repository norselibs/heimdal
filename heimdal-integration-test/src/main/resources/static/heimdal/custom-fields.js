/**
 * Project-specific field components for the integration test.
 *
 * Picked up automatically by the generateFormBuilder task alongside
 * heimdal-core's fields.js — no registration needed anywhere else.
 */

// ---------------------------------------------------------------------------
// hm-rating-field  —  1–5 star rating backed by an integer
// ---------------------------------------------------------------------------

class HmRatingField extends HTMLElement {
    static heimdal = { type: 'integer' };

    get value() {
        const checked = this.querySelector('input[type=radio]:checked');
        return checked ? checked.value : '';
    }

    setErrors(messages) {
        const el = this.querySelector('.hm-error');
        if (!el) return;
        el.textContent = messages[0] ?? '';
        el.hidden = messages.length === 0;
    }

    connectedCallback() { this._render(); }

    _render() {
        const name     = this.getAttribute('name') ?? '';
        const label    = this.getAttribute('label') ?? '';
        const current  = this.getAttribute('value') ?? '';
        const required = this.hasAttribute('required');

        const stars = [1, 2, 3, 4, 5].map(n => `
            <label>
                <input type="radio" name="${name}" value="${n}"${String(n) === current ? ' checked' : ''}>
                ★
            </label>`).join('');

        this.innerHTML = `
            <div class="hm-field">
                <span class="hm-label">${label}${required ? ' <span aria-hidden="true">*</span>' : ''}</span>
                <div class="hm-stars">${stars}</div>
                <span class="hm-error" hidden></span>
            </div>`;
    }
}
customElements.define('hm-rating-field', HmRatingField);
