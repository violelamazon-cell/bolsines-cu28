package ar.edu.utn.frc.dsi.bolsines.repositorios;

import ar.edu.utn.frc.dsi.bolsines.entidades.ComisionMedica;
import ar.edu.utn.frc.dsi.bolsines.entidades.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {
    Optional<Empleado> findByEmail(String email);
    List<Empleado> findByAsignadoA(ComisionMedica asignadoA);
}

