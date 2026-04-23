#import sys: inputs

// Page setup
#set page(
  paper: "a4",
  margin: (top: 2.5cm, bottom: 2cm, left: 1.5cm, right: 1.5cm),
  header: context {
    if counter(page).get().first() > 1 [
      #set text(8pt, fill: luma(120))
      #inputs.title
      #h(1fr)
      #inputs.generated
    ]
  },
  footer: [
    #set text(8pt, fill: luma(120))
    #h(1fr)
    Page #context counter(page).display("1 / 1", both: true)
  ],
)

#set text(font: "Liberation Sans", 9pt)

// Title block
#align(center)[
  #text(18pt, weight: "bold")[#inputs.title]
  #v(4pt)
  #text(9pt, fill: luma(100))[Generated: #inputs.generated]
]

#v(12pt)

// Main table
#let headers = inputs.headers
#let rows = inputs.rows
#let col_count = headers.len()

#table(
  columns: headers.map(_ => 1fr),
  inset: 6pt,
  stroke: 0.5pt + luma(200),
  fill: (_, y) => if y == 0 { rgb("#2c3e50") } else if calc.odd(y) { luma(245) } else { white },

  // Header row
  ..headers.map(h => text(weight: "bold", fill: if true { white } else { black }, size: 8pt)[#h]),

  // Data rows
  ..rows.map(row => row.map(cell => text(size: 7.5pt)[#cell])).flatten(),
)

#v(8pt)
#text(9pt, fill: luma(100))[Total: #rows.len() rows]

// Appendix section (if present)
#if "appendix_title" in inputs and inputs.appendix_rows.len() > 0 [
  #pagebreak()

  #align(center)[
    #text(14pt, weight: "bold")[#inputs.appendix_title]
  ]
  #v(12pt)

  #let app_headers = inputs.appendix_headers
  #let app_rows = inputs.appendix_rows

  #table(
    columns: app_headers.map(_ => 1fr),
    inset: 6pt,
    stroke: 0.5pt + luma(200),
    fill: (_, y) => if y == 0 { rgb("#2c3e50") } else if calc.odd(y) { luma(245) } else { white },

    ..app_headers.map(h => text(weight: "bold", fill: white, size: 8pt)[#h]),
    ..app_rows.map(row => row.map(cell => text(size: 7.5pt)[#cell])).flatten(),
  )

  #v(8pt)
  #text(9pt, fill: luma(100))[Total: #app_rows.len() rows]
]
