package de.sedzkitchens.customer;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

	CustomerResponse toResponse(Customer customer);

	// Copies the request onto a new or existing customer; id and timestamps stay under the database's control
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	void update(@MappingTarget Customer customer, CustomerRequest request);

	@Mapping(target = "createdByName",
			expression = "java(contact.getCreatedBy().getFirstName() + \" \" + contact.getCreatedBy().getLastName())")
	ContactResponse toResponse(CustomerContact contact);

}
