package io.github.kottzi.gamepicker.auth.domain.repository;

import io.github.kottzi.gamepicker.auth.domain.model.AppUser;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface AppUserRepository extends CrudRepository<AppUser, Long> {

    Optional<AppUser> findBySteamId(String steamId);
}
