import { backendAssetUrl } from '@/api/http'

export const DEFAULT_RECIPE_IMAGE = '/images/recipes/default-recipe.svg'
export const DEFAULT_INGREDIENT_ICON = '/images/ingredients/ingredient-default.svg'

const recipeImagePositionMap: Record<string, string> = {
  '/images/recipes/chicken-broccoli-bowl.jpg': 'center 52%',
  '/images/recipes/shrimp-tofu-soup.jpg': 'center 50%',
  '/images/recipes/tomato-egg-oatmeal.jpg': 'center 58%',
  '/images/recipes/tuna-lettuce-salad.jpg': 'center 46%',
  '/images/recipes/cod-asparagus.jpg': 'center 48%',
  '/images/recipes/egg-tofu-custard.jpg': 'center 54%',
  '/images/recipes/tomato-egg.jpg': 'center 55%',
  '/images/recipes/beef-potato-carrot.jpg': 'center 48%',
  '/images/recipes/mushroom-tofu-rice.jpg': 'center 50%',
  '/images/recipes/salmon-quinoa-bowl.jpg': 'center 52%',
  '/images/recipes/light-kungpao-chicken.jpg': 'center 50%',
  '/images/recipes/seaweed-egg-noodle.jpg': 'center 54%',
  '/images/recipes/blackpepper-beef-brownrice.jpg': 'center 46%',
  '/images/recipes/chicken-avocado-wrap.jpg': 'center 47%',
  '/images/recipes/beef-egg-quinoa.jpg': 'center 49%',
  '/images/recipes/chicken-tofu-protein.jpg': 'center 46%',
  '/images/recipes/tuna-corn-sandwich.jpg': 'center 49%',
  '/images/recipes/shrimp-egg-pasta.jpg': 'center 50%',
  '/images/recipes/chicken-pumpkin-salad.jpg': 'center 48%',
  '/images/recipes/purple-sweet-potato-yogurt-bowl.jpg': 'center 50%',
  '/images/recipes/cucumber-shrimp-egg-cup.jpg': 'center 50%',
  '/images/recipes/tomato-beef-soba-noodle.jpg': 'center 47%',
  '/images/recipes/pumpkin-egg-grain-porridge.jpg': 'center 54%',
  '/images/recipes/pork-cabbage-fried-rice.jpg': 'center 48%',
  '/images/recipes/cumin-beef-sweet-potato-bowl.jpg': 'center 50%',
  '/images/recipes/shrimp-avocado-soba-noodle.jpg': 'center 50%',
  '/images/recipes/beef-edamame-quinoa-bowl.jpg': 'center 50%',
  '/images/recipes/basa-sweet-potato-training-plate.jpg': 'center 50%',
}

const ingredientIconMap: Record<string, string> = {
  鸡胸肉: '/images/ingredients/chicken-breast.svg',
  鸡肉: '/images/ingredients/chicken-breast.svg',
  牛肉: '/images/ingredients/beef.svg',
  虾仁: '/images/ingredients/shrimp.svg',
  虾皮: '/images/ingredients/shrimp.svg',
  鸡蛋: '/images/ingredients/egg.svg',
  豆腐: '/images/ingredients/tofu.svg',
  西兰花: '/images/ingredients/broccoli.svg',
  西蓝花: '/images/ingredients/broccoli.svg',
  番茄: '/images/ingredients/tomato.svg',
  菠菜: '/images/ingredients/spinach.svg',
  彩椒: '/images/ingredients/bell-pepper.svg',
  玉米: '/images/ingredients/corn.svg',
  牛油果: '/images/ingredients/avocado.svg',
  藜麦: '/images/ingredients/quinoa.svg',
  洋葱: '/images/ingredients/onion.svg',
  胡萝卜: '/images/ingredients/carrot.svg',
  香菇: '/images/ingredients/mushroom.svg',
  生菜: '/images/ingredients/lettuce.svg',
  金枪鱼: '/images/ingredients/fish.svg',
  三文鱼: '/images/ingredients/fish.svg',
  鳕鱼: '/images/ingredients/fish.svg',
  巴沙鱼: '/images/ingredients/basa-fish.svg',
  鸡腿肉: '/images/ingredients/chicken-thigh.svg',
  猪里脊: '/images/ingredients/pork.svg',
  牛奶: '/images/ingredients/milk.svg',
  无糖酸奶: '/images/ingredients/yogurt.svg',
  酸奶: '/images/ingredients/yogurt.svg',
  毛豆: '/images/ingredients/edamame.svg',
  白菜: '/images/ingredients/cabbage.svg',
  南瓜: '/images/ingredients/pumpkin.svg',
  冬瓜: '/images/ingredients/winter-melon.svg',
  西葫芦: '/images/ingredients/zucchini.svg',
  红薯: '/images/ingredients/sweet-potato.svg',
  紫薯: '/images/ingredients/purple-sweet-potato.svg',
  荞麦面: '/images/ingredients/soba-noodle.svg',
  杂粮饭: '/images/ingredients/grain-rice.svg',
}

function normalizeIngredientName(name?: string) {
  return (name ?? '').trim().replace(/\s+/g, '')
}

export function resolveRecipeImage(url?: string) {
  return backendAssetUrl(url) || DEFAULT_RECIPE_IMAGE
}

export function resolveRecipeImagePosition(url?: string) {
  const normalized = backendAssetUrl(url)
  return recipeImagePositionMap[normalized] ?? 'center center'
}

export function resolveIngredientIcon(name?: string) {
  const normalized = normalizeIngredientName(name)
  return ingredientIconMap[normalized] ?? DEFAULT_INGREDIENT_ICON
}

export function replaceImageWithFallback(event: Event, fallback = DEFAULT_RECIPE_IMAGE) {
  const target = event.target
  if (!(target instanceof HTMLImageElement)) return
  target.onerror = null
  target.src = fallback
}
