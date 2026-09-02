package com.wherewear.backend.service;

import com.google.firebase.auth.FirebaseToken;
import com.wherewear.backend.model.AppUser;
import com.wherewear.backend.repository.AppUserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final AppUserRepository appUserRepository;
    private final CategoryTemplateService categoryTemplateService;

    public UserService(AppUserRepository appUserRepository, CategoryTemplateService categoryTemplateService) {
        this.appUserRepository = appUserRepository;
        this.categoryTemplateService = categoryTemplateService;
    }

    /**
     * Ensures a users/{uid} doc exists for this Firebase-authenticated caller.
     * On first sight of a user, also seeds their starter category templates
     * (CategoryCatalog defaults) so packing-list generation has something to
     * diff against right away.
     */
    public void ensureUserExists(FirebaseToken token) {
        String uid = token.getUid();
        if (appUserRepository.findById(uid) != null) {
            return;
        }
        AppUser user = new AppUser();
        user.setId(uid);
        user.setEmail(token.getEmail());
        user.setDisplayName(token.getName());
        appUserRepository.create(user);
        categoryTemplateService.seedDefaultsForNewUser(uid);
    }
}
