package com.shanzai.recipe.modules.shopping;

import com.shanzai.recipe.common.ApiResponse;
import com.shanzai.recipe.security.JwtUser;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/shopping-lists")
public class ShoppingListController {
    private final ShoppingListService shoppingListService;

    public ShoppingListController(ShoppingListService shoppingListService) {
        this.shoppingListService = shoppingListService;
    }

    @PostMapping
    public ApiResponse<ShoppingListResponse> createShoppingList(
        Authentication authentication,
        @Valid @RequestBody ShoppingListCreateRequest request
    ) {
        return ApiResponse.ok(shoppingListService.createShoppingList(currentUserId(authentication), request));
    }

    @GetMapping
    public ApiResponse<List<ShoppingListSummaryResponse>> listShoppingLists(Authentication authentication) {
        return ApiResponse.ok(shoppingListService.listShoppingLists(currentUserId(authentication)));
    }

    @GetMapping("/{id}")
    public ApiResponse<ShoppingListResponse> getShoppingList(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.ok(shoppingListService.getShoppingList(currentUserId(authentication), id));
    }

    @PatchMapping("/{listId}/items/{itemId}")
    public ApiResponse<ShoppingListItemResponse> updateItemChecked(
        Authentication authentication,
        @PathVariable Long listId,
        @PathVariable Long itemId,
        @Valid @RequestBody ShoppingListCheckRequest request
    ) {
        return ApiResponse.ok(shoppingListService.updateItemChecked(
            currentUserId(authentication),
            listId,
            itemId,
            request.checked()
        ));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteShoppingList(Authentication authentication, @PathVariable Long id) {
        shoppingListService.deleteShoppingList(currentUserId(authentication), id);
        return ApiResponse.ok(null);
    }

    private Long currentUserId(Authentication authentication) {
        JwtUser jwtUser = (JwtUser) authentication.getPrincipal();
        return jwtUser.userId();
    }
}
