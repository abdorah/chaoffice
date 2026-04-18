The goal of this project is to prepare the context to generate the specs for an inventory management project that works for both desktop and mobile applications.

The way this project is created is using qleany, all commands are going to be run using `uv run qleany`.

The file [qleany docs](./qleany_docs.md) contains the latest qleany documentation that the llm should learn from.
The file [slint full docs](./slint_full_docs.md) contains the latest slint documentation.

The second step is to create a security authorization and authentication mechanism. Currently, Qleany does an excellent job of scaffolding the "boring" parts of an application (DTOs, Repositories, Use Cases). However, most real-world applications require a security layer to ensure that only authorized users can execute specific Features or UseCases.

I propose adding a crate to Qleany generated code that allows developers to define roles and permissions, which then generates:

- A central Role enum.
- A SecurityContext extractor/structure.
- Procedural Macros (or generated wrappers) to enforce these roles on generated Use Cases. This was inspired by spring security expression language SpEL in the java world, axum_grant macros and actix_web_grants macros in the rust world.


Your artifacts that you sould create are the following:
- qleany document.
- A reference folder containing the component needed by slint ui inspired by its documentation and this gallery example: https://github.com/slint-ui/slint/tree/master/examples/gallery, which was cloned to here: [gallery](./slint-gallery/examples/gallery)

The project structure is this:

Inventory management application having these entities product , category, supplier, manager, deal , Location, contact:
product: id, name , reference, description, quantity, price unit, category id, supplier id, location id
category: id , name description, category id
person: id, name, type id
type: id, name: either a manager or a supplier
contact,: id, phone, mail, person id
deal: id, product id, supplier id, manager id, frequency: frequency is if the deal is recurrent for menthly basis for example
location: id, address, geoloc x, geoloc y, name, size or capacity

You are allowed to change this architecture as a senior software engineer and correct its short commings. These are only entities, you have to infer features and unit of work and processes related to them.

Step three, is to use torsodb with lisql as a database offering online and offline capabilities out of the box.