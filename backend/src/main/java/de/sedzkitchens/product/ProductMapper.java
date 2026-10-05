package de.sedzkitchens.product;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductMapper {

	@Mapping(target = "supplierId", source = "supplier.id")
	@Mapping(target = "supplierName", source = "supplier.name")
	ProductResponse toResponse(Product product);

	// The supplier is looked up and set by the service
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "supplier", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	void update(@MappingTarget Product product, ProductRequest request);

}
