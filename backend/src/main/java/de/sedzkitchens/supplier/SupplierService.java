package de.sedzkitchens.supplier;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.sedzkitchens.common.ConflictException;
import de.sedzkitchens.common.NotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupplierService {

	private final SupplierRepository supplierRepository;

	public List<SupplierResponse> findAll() {
		return supplierRepository.findAll(Sort.by("name")).stream().map(SupplierResponse::from).toList();
	}

	@Transactional
	public SupplierResponse create(SupplierRequest request) {
		if (supplierRepository.existsByNameIgnoreCase(request.name())) {
			throw new ConflictException("A supplier with this name already exists");
		}
		Supplier supplier = new Supplier();
		apply(supplier, request);
		return SupplierResponse.from(supplierRepository.save(supplier));
	}

	@Transactional
	public SupplierResponse update(Long id, SupplierRequest request) {
		Supplier supplier = supplierRepository.findById(id)
			.orElseThrow(() -> new NotFoundException("Supplier not found"));
		if (supplierRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
			throw new ConflictException("A supplier with this name already exists");
		}
		apply(supplier, request);
		return SupplierResponse.from(supplier);
	}

	private void apply(Supplier supplier, SupplierRequest request) {
		supplier.setName(request.name());
		supplier.setEmail(request.email());
		supplier.setPhone(request.phone());
	}

}
