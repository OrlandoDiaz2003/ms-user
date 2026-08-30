package com.pcmarketbuilder.user_service.Repository;

import com.pcmarketbuilder.user_service.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByAzureOid(String azureOid);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);
}
