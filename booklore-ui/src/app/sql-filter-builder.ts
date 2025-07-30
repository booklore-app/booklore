import {AbstractControl, FormArray} from '@angular/forms';

export class SqlFilterBuilder {
  static toSQL(group: AbstractControl): string {
    const condition: 'and' | 'or' = group.get('join')?.value ?? 'and';
    const rules = group.get('rules') as FormArray;

    const parts: string[] = [];

    rules.controls.forEach((ctrl: AbstractControl) => {
      if (this.isGroup(ctrl)) {
        parts.push(`(${this.toSQL(ctrl)})`);
        return;
      }

      const field = ctrl.get('field')?.value;
      const operator = ctrl.get('operator')?.value;
      const value = ctrl.get('value')?.value;
      const valueStart = ctrl.get('valueStart')?.value;
      const valueEnd = ctrl.get('valueEnd')?.value;

      if (!field || !operator) return;

      if (operator === 'in_between') {
        parts.push(`${field} BETWEEN ${this.formatSQLValue(valueStart)} AND ${this.formatSQLValue(valueEnd)}`);
        return;
      }

      if (field === 'library') {
        parts.push(`${field} = ${value}`);
        return;
      }

      let sqlOperator = '';
      let sqlValue = '';

      switch (operator) {
        case 'equals':
          sqlOperator = '=';
          sqlValue = this.formatSQLValue(value);
          break;
        case 'not_equals':
          sqlOperator = '!=';
          sqlValue = this.formatSQLValue(value);
          break;
        case 'contains':
          sqlOperator = 'LIKE';
          sqlValue = `'%${String(value).replace(/'/g, "''")}%'`;
          break;
        case 'starts_with':
          sqlOperator = 'LIKE';
          sqlValue = `'${String(value).replace(/'/g, "''")}%'`;
          break;
        case 'ends_with':
          sqlOperator = 'LIKE';
          sqlValue = `'%${String(value).replace(/'/g, "''")}'`;
          break;
        case 'greater_than':
          sqlOperator = '>';
          sqlValue = this.formatSQLValue(value);
          break;
        case 'less_than':
          sqlOperator = '<';
          sqlValue = this.formatSQLValue(value);
          break;
        case 'is_empty':
          parts.push(`(${field} IS NULL OR ${field} = '')`);
          return;
        case 'is_not_empty':
          parts.push(`(${field} IS NOT NULL AND ${field} != '')`);
          return;
        case 'in_list':
          sqlOperator = 'IN';
          sqlValue = `(${(value as string)?.split(',').map(v => this.formatSQLValue(v.trim())).join(', ')})`;
          break;
        case 'not_in_list':
          sqlOperator = 'NOT IN';
          sqlValue = `(${(value as string)?.split(',').map(v => this.formatSQLValue(v.trim())).join(', ')})`;
          break;
        default:
          return;
      }

      parts.push(`${field} ${sqlOperator} ${sqlValue}`);
    });

    return parts.join(` ${condition.toUpperCase()} `);
  }

  private static isGroup(ctrl: AbstractControl): boolean {
    return ctrl.get('rules') instanceof FormArray;
  }

  private static formatSQLValue(val: any): string {
    if (val == null || val === '') return 'NULL';

    // Handle boolean
    if (typeof val === 'boolean') return val ? 'true' : 'false';

    // Handle numeric
    if (!isNaN(val) && typeof val !== 'object') return String(val);

    // Handle dates
    const date = new Date(val);
    if (!isNaN(date.getTime())) {
      return `'${date.toISOString().slice(0, 10)}'`;
    }

    // Handle strings (with escaped single quotes)
    return `'${String(val).replace(/'/g, "''")}'`;
  }
}
