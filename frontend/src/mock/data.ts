import type { Profile, RecipeCardModel } from '@/types'

export const demoProfile: Profile = {
  heightCm: 165,
  weightKg: 55,
  age: 26,
  gender: '女',
  dietGoal: 'FAT_LOSS',
  tastePreferences: ['清淡', '健康', '不油腻'],
  avoidIngredients: ['香菜'],
  allergyIngredients: [],
  cookingTimePreference: 30,
}

export const recipes: RecipeCardModel[] = [
  {
    id: 1,
    name: '鸡胸肉西兰花轻食碗',
    imageUrl:
      'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=900&q=80',
    score: 92,
    calories: 360,
    protein: 36,
    time: 25,
    tags: ['高蛋白', '低脂', '已有食材 3/5'],
    reason: '鸡胸肉和西兰花命中你的已有食材，热量稳定，适合减脂控热量。',
  },
  {
    id: 2,
    name: '番茄虾仁燕麦粥',
    imageUrl:
      'https://images.unsplash.com/photo-1547592166-23ac45744acd?auto=format&fit=crop&w=900&q=80',
    score: 86,
    calories: 310,
    protein: 22,
    time: 20,
    tags: ['清爽', '高纤维', '20 分钟'],
    reason: '番茄和虾仁组合清爽，蛋白质充足，适合晚餐轻负担。',
  },
  {
    id: 3,
    name: '虾仁豆腐汤',
    imageUrl:
      'https://images.unsplash.com/photo-1603105037880-880cd4edfb0d?auto=format&fit=crop&w=900&q=80',
    score: 82,
    calories: 280,
    protein: 26,
    time: 18,
    tags: ['低热量', '暖胃', '少油'],
    reason: '豆腐和虾仁补充优质蛋白，做法简单，适合工作日晚餐。',
  },
  {
    id: 4,
    name: '藜麦牛肉沙拉',
    imageUrl:
      'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=900&q=80',
    score: 80,
    calories: 420,
    protein: 32,
    time: 30,
    tags: ['增肌友好', '饱腹', '膳食纤维'],
    reason: '藜麦提供复合碳水，牛肉补充蛋白，适合训练日午餐。',
  },
]

export const quickIngredients = [
  '鸡胸肉',
  '番茄',
  '牛肉',
  '藜麦',
  '鸡蛋',
  '豆腐',
  '西兰花',
  '虾仁',
]
