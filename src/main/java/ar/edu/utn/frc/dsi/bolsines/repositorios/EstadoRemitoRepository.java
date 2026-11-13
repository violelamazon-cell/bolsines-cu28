package ar.edu.utn.frc.dsi.bolsines.repositorios;

import ar.edu.utn.frc.dsi.bolsines.entidades.EstadoRemito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstadoRemitoRepository extends JpaRepository<EstadoRemito, Long> {
    Optional<EstadoRemito> findByNombre(String nombre);
}

