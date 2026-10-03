// Refleja org.springframework.data.domain.Page<T> (Spring Data)

export interface Pagina<T> {
  content: T[]
  page: {
    number: number
    size: number
    totalElements: number
    totalPages: number
  }
}
