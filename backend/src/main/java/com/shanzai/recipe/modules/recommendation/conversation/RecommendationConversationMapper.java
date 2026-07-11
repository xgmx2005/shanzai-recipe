package com.shanzai.recipe.modules.recommendation.conversation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RecommendationConversationMapper extends BaseMapper<RecommendationConversationEntity> {
    @Select("""
            SELECT *
            FROM recommendation_conversation
            WHERE id = #{id}
              AND user_id = #{userId}
            FOR UPDATE
            """)
    RecommendationConversationEntity selectOwnedByIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);
}
