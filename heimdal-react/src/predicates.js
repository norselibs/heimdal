/** Client-side predicate evaluation — mirrors hm-form.js #evaluate. */
export function evaluate(pred, values) {
  if (!pred) return true;
  switch (pred.op) {
    case 'eq':  return (values[pred.field] ?? '') === String(pred.value);
    case 'neq': return (values[pred.field] ?? '') !== String(pred.value);
    case 'in':  return pred.values.map(String).includes(values[pred.field] ?? '');
    case 'lte': return (values[pred.field] ?? '') <= String(pred.value);
    case 'gte': return (values[pred.field] ?? '') >= String(pred.value);
    case 'lt':  return (values[pred.field] ?? '') <  String(pred.value);
    case 'gt':  return (values[pred.field] ?? '') >  String(pred.value);
    // allRequiredValid is evaluated in HeimdalForm directly
    default: return true;
  }
}

/** Flatten all field definitions out of items (including inside sections). */
export function flattenFields(items = []) {
  return items.flatMap(item =>
    item.section ? flattenFields(item.items) : [item]
  ).filter(i => i.name);
}
