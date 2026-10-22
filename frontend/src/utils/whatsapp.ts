/**
 * Enlace que abre WhatsApp con el mensaje ya escrito para ese numero (no hace
 * falta API ni cuenta especial: se envia desde el WhatsApp del celular).
 * Numeros peruanos de 9 digitos reciben el codigo de pais 51.
 */
export function enlaceWhatsApp(telefono: string, mensaje: string): string | null {
  let numero = telefono.replace(/\D/g, '')
  if (numero.length === 9) numero = '51' + numero
  if (numero.length < 10) return null
  return `https://wa.me/${numero}?text=${encodeURIComponent(mensaje)}`
}
