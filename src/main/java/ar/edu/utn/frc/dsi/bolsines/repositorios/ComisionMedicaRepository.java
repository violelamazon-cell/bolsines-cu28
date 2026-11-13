package ar.edu.utn.frc.dsi.bolsines.repositorios;

import ar.edu.utn.frc.dsi.bolsines.entidades.ComisionMedica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ComisionMedicaRepository extends JpaRepository<ComisionMedica, Long> {
    Optional<ComisionMedica> findByCodigo(String codigo);
    Optional<ComisionMedica> findByNombre(String nombre);
}

