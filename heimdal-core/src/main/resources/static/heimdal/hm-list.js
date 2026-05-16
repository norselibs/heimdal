/**
 * hm-list — Heimdal list component.
 *
 * Reads the JSON list definition from its child <script type="application/json">
 * and renders a table. The wire format mirrors hm-form's schema embedding:
 *
 *   columns    array of { name, label, component, sortable }
 *   rows       array of { [colName]: value, ..., _rowActions: [{ label, url }] }
 *   actions    array of { label, url } — page-level buttons (e.g. "New")
 *   pagination reserved for future sort/page support — ignored for now
 */
class HmList extends HTMLElement {
    connectedCallback() {
        const scriptEl = this.querySelector('script[type="application/json"]');
        if (!scriptEl) return;
        const schema = JSON.parse(scriptEl.textContent);
        scriptEl.remove();
        this.innerHTML = this.#render(schema);
    }

    #render(schema) {
        const headerActions = (schema.actions ?? [])
            .map(a => `<a href="${a.url}" class="hm-list-action">${a.label}</a>`)
            .join('');

        const headerRow = schema.columns
            .map(c => `<th>${c.label}</th>`)
            .join('') + (schema.rows.some(r => r._rowActions?.length) ? '<th></th>' : '');

        const bodyRows = (schema.rows ?? []).map(row => {
            const cells = schema.columns
                .map(c => `<td>${row[c.name] ?? ''}</td>`)
                .join('');
            const rowActions = (row._rowActions ?? [])
                .map(a => `<a href="${a.url}" class="hm-row-action">${a.label}</a>`)
                .join('');
            const actionCell = rowActions ? `<td class="hm-row-actions">${rowActions}</td>` : '';
            return `<tr>${cells}${actionCell}</tr>`;
        }).join('');

        return `
            <div class="hm-list-toolbar">${headerActions}</div>
            <table class="hm-table">
                <thead><tr>${headerRow}</tr></thead>
                <tbody>${bodyRows}</tbody>
            </table>`;
    }
}
customElements.define('hm-list', HmList);
