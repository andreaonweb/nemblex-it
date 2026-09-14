import { esPaginatorIntl } from './paginator-intl-es';

describe('esPaginatorIntl', () => {
  it('translates the static paginator labels to Spanish', () => {
    const intl = esPaginatorIntl();

    expect(intl.itemsPerPageLabel).toBe('Elementos por página:');
    expect(intl.nextPageLabel).toBe('Página siguiente');
    expect(intl.previousPageLabel).toBe('Página anterior');
    expect(intl.firstPageLabel).toBe('Primera página');
    expect(intl.lastPageLabel).toBe('Última página');
  });

  it('renders the range label as "start – end de total"', () => {
    const intl = esPaginatorIntl();

    expect(intl.getRangeLabel(0, 20, 31)).toBe('1 – 20 de 31');
    expect(intl.getRangeLabel(1, 20, 31)).toBe('21 – 31 de 31');
  });

  it('renders "0 de N" when the length or page size is zero', () => {
    const intl = esPaginatorIntl();

    expect(intl.getRangeLabel(0, 20, 0)).toBe('0 de 0');
    expect(intl.getRangeLabel(0, 0, 10)).toBe('0 de 10');
  });
});
