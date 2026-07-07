package com.shanzai.recipe.modules.profile;

import com.shanzai.recipe.common.DietGoal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {
    @Mock
    private ProfileMapper profileMapper;

    @InjectMocks
    private ProfileService profileService;

    @Test
    void saveProfileCalculatesBmiAndStoresListFields() {
        when(profileMapper.selectOne(any())).thenReturn(null);
        when(profileMapper.insert(any(ProfileEntity.class))).thenAnswer(invocation -> {
            ProfileEntity profile = invocation.getArgument(0);
            profile.setId(10L);
            return 1;
        });

        ProfileResponse response = profileService.saveProfile(
            7L,
            new ProfileRequest(
                "FEMALE",
                20,
                new BigDecimal("170"),
                new BigDecimal("65"),
                DietGoal.FAT_LOSS,
                List.of("清淡", "低脂"),
                List.of("辣椒"),
                List.of(),
                25
            )
        );

        ArgumentCaptor<ProfileEntity> captor = ArgumentCaptor.forClass(ProfileEntity.class);
        verify(profileMapper).insert(captor.capture());
        ProfileEntity saved = captor.getValue();

        assertEquals(7L, saved.getUserId());
        assertEquals(new BigDecimal("22.49"), saved.getBmi());
        assertEquals("清淡,低脂", saved.getTastePreferences());
        assertEquals("辣椒", saved.getAvoidIngredients());
        assertEquals(DietGoal.FAT_LOSS.name(), saved.getDietGoal());
        assertEquals(List.of("清淡", "低脂"), response.tastePreferences());
    }
}
