package ar.edu.utn.frc.dsi.bolsines.repositorios;

import ar.edu.utn.frc.dsi.bolsines.entidades.EstadoBolsin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstadoBolsinRepository extends JpaRepository<EstadoBolsin, Long> {
    Optional<EstadoBolsin> findByNombre(String nombre);
}

