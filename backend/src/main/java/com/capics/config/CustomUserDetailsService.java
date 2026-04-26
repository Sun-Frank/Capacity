package com.capics.config;

import com.capics.entity.SysUser;
import com.capics.entity.SysUserRole;
import com.capics.repository.SysRoleRepository;
import com.capics.repository.SysUserRoleRepository;
import com.capics.repository.SysUserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final SysUserRepository userRepository;
    private final SysUserRoleRepository userRoleRepository;
    private final SysRoleRepository roleRepository;

    public CustomUserDetailsService(SysUserRepository userRepository,
                                    SysUserRoleRepository userRoleRepository,
                                    SysRoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        List<GrantedAuthority> authorities = loadAuthorities(user.getId());

        return new User(
                user.getUsername(),
                user.getPassword(),
                user.getEnabled() != null && user.getEnabled(),
                true,
                true,
                true,
                authorities
        );
    }

    private List<GrantedAuthority> loadAuthorities(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        List<SysUserRole> userRoles = userRoleRepository.findByUserId(userId);
        if (userRoles.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> roleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }

        return roleRepository.findByIdIn(roleIds).stream()
                .map(role -> role.getRoleCode() == null ? null : role.getRoleCode().trim())
                .filter(code -> code != null && !code.isEmpty())
                .map(code -> code.startsWith("ROLE_") ? code : "ROLE_" + code)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}
