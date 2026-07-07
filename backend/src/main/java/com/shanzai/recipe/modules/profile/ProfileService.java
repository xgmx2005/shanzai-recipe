package com.shanzai.recipe.modules.profile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shanzai.recipe.common.DietGoal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ProfileService {
    private static final BigDecimal BMI_UNDERWEIGHT = new BigDecimal("18.5");
    private static final BigDecimal BMI_OVERWEIGHT = new BigDecimal("24");
    private static final BigDecimal BMI_OBESE = new BigDecimal("28");

    private final ProfileMapper profileMapper;

    public ProfileService(ProfileMapper profileMapper) {
        this.profileMapper = profileMapper;
    }

    public ProfileResponse getProfile(Long userId) {
        return toResponse(findByUserId(userId));
    }

    @Transactional
    public ProfileResponse saveProfile(Long userId, ProfileRequest request) {
        ProfileEntity existing = findByUserId(userId);
        ProfileEntity profile = existing == null ? new ProfileEntity() : existing;
        profile.setUserId(userId);
        applyRequest(profile, request);

        if (existing == null) {
            profileMapper.insert(profile);
        } else {
            profileMapper.updateById(profile);
        }
        return toResponse(profile);
    }

    public ProfileSummaryResponse getSummary(Long userId) {
        ProfileEntity profile = findByUserId(userId);
        if (profile == null) {
            return new ProfileSummaryResponse(false, DietGoal.BALANCED.name(), null, "未填写", null, null);
        }
        return new ProfileSummaryResponse(
            true,
            profile.getDietGoal(),
            profile.getBmi(),
            bmiStatus(profile.getBmi()),
            profile.getDailyCalorieTarget(),
            profile.getCookingTimePreference()
        );
    }

    private ProfileEntity findByUserId(Long userId) {
        return profileMapper.selectOne(new LambdaQueryWrapper<ProfileEntity>().eq(ProfileEntity::getUserId, userId));
    }

    private void applyRequest(ProfileEntity profile, ProfileRequest request) {
        DietGoal dietGoal = request.dietGoal() == null ? DietGoal.BALANCED : request.dietGoal();
        profile.setGender(cleanText(request.gender()));
        profile.setAge(request.age());
        profile.setHeightCm(request.heightCm());
        profile.setWeightKg(request.weightKg());
        profile.setBmi(calculateBmi(request.heightCm(), request.weightKg()));
        profile.setDietGoal(dietGoal.name());
        profile.setTastePreferences(joinList(request.tastePreferences()));
        profile.setAvoidIngredients(joinList(request.avoidIngredients()));
        profile.setAllergyIngredients(joinList(request.allergyIngredients()));
        profile.setCookingTimePreference(request.cookingTimePreference());
        profile.setDailyCalorieTarget(estimateDailyCalorieTarget(request, dietGoal));
    }

    private BigDecimal calculateBmi(BigDecimal heightCm, BigDecimal weightKg) {
        if (heightCm == null || weightKg == null) {
            return null;
        }
        if (heightCm.compareTo(BigDecimal.ZERO) <= 0 || weightKg.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return BmiCalculator.calculate(heightCm, weightKg);
    }

    private Integer estimateDailyCalorieTarget(ProfileRequest request, DietGoal dietGoal) {
        if (request.age() == null || request.heightCm() == null || request.weightKg() == null) {
            return switch (dietGoal) {
                case FAT_LOSS -> 1600;
                case MUSCLE_GAIN -> 2400;
                case BALANCED -> 2000;
            };
        }

        double offset = switch (String.valueOf(request.gender()).toUpperCase()) {
            case "MALE" -> 5;
            case "FEMALE" -> -161;
            default -> -78;
        };
        double bmr = 10 * request.weightKg().doubleValue()
            + 6.25 * request.heightCm().doubleValue()
            - 5 * request.age()
            + offset;
        double target = bmr * 1.35;
        target += switch (dietGoal) {
            case FAT_LOSS -> -300;
            case MUSCLE_GAIN -> 250;
            case BALANCED -> 0;
        };
        int rounded = (int) Math.round(target / 50.0) * 50;
        return Math.max(1200, rounded);
    }

    private ProfileResponse toResponse(ProfileEntity profile) {
        if (profile == null) {
            return null;
        }
        return new ProfileResponse(
            profile.getId(),
            profile.getUserId(),
            profile.getGender(),
            profile.getAge(),
            profile.getHeightCm(),
            profile.getWeightKg(),
            profile.getBmi(),
            profile.getDietGoal(),
            splitList(profile.getTastePreferences()),
            splitList(profile.getAvoidIngredients()),
            splitList(profile.getAllergyIngredients()),
            profile.getCookingTimePreference(),
            profile.getDailyCalorieTarget(),
            profile.getUpdatedAt()
        );
    }

    private String joinList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .distinct()
            .collect(Collectors.joining(","));
    }

    private List<String> splitList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(item -> !item.isBlank())
            .toList();
    }

    private String cleanText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String bmiStatus(BigDecimal bmi) {
        if (bmi == null) {
            return "未填写";
        }
        if (bmi.compareTo(BMI_UNDERWEIGHT) < 0) {
            return "偏瘦";
        }
        if (bmi.compareTo(BMI_OVERWEIGHT) < 0) {
            return "正常";
        }
        if (bmi.compareTo(BMI_OBESE) < 0) {
            return "超重";
        }
        return "肥胖";
    }
}
