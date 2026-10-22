import { describe, expect, it } from 'vitest'
import { enlaceWhatsApp } from './whatsapp'

describe('enlaceWhatsApp', () => {
  it('agrega el 51 a un celular peruano y escribe el mensaje', () => {
    expect(enlaceWhatsApp('982 616 709', 'Su chapa está lista')).toBe(
      'https://wa.me/51982616709?text=Su%20chapa%20est%C3%A1%20lista')
  })
  it('respeta un número que ya trae el código de país', () => {
    expect(enlaceWhatsApp('+51 982616709', 'x')).toBe('https://wa.me/51982616709?text=x')
  })
  it('un número incompleto no da enlace', () => {
    expect(enlaceWhatsApp('12345', 'x')).toBeNull()
  })
})
