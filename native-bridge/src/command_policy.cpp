#include "xptool/bridge/command_policy.hpp"

#include <algorithm>
#include <array>
#include <cctype>
#include <string>

namespace xptool::bridge {
namespace {

constexpr std::array<const char*, 33> kSupportedCommandTypes = {
    "OPEN_BANK",
    "BANK_OPEN_SAFE",
    "ENTER_BANK_PIN",
    "SEARCH_BANK_ITEM",
    "DEPOSIT_ITEM",
    "DEPOSIT_ALL_EXCEPT",
    "BANK_DEPOSIT_ALL_EXCEPT_TOOL_SAFE",
    "WITHDRAW_ITEM",
    "BANK_WITHDRAW_LOGS_SAFE",
    "WOODCUT_CHOP_NEAREST_TREE_SAFE",
    "MINE_NEAREST_ROCK_SAFE",
    "FISH_NEAREST_SPOT_SAFE",
    "WALK_TO_WORLDPOINT_SAFE",
    "CAMERA_NUDGE_SAFE",
    "SET_FISHING_IDLE_MODE_SAFE",
    "COMBAT_ATTACK_NEAREST_NPC_SAFE",
    "NPC_CONTEXT_MENU_TEST",
    "SCENE_OBJECT_ACTION_SAFE",
    "AGILITY_OBSTACLE_ACTION_SAFE",
    "GROUND_ITEM_ACTION_SAFE",
    "SHOP_BUY_ITEM_SAFE",
    "WORLD_HOP_SAFE",
    "DROP_START_SESSION",
    "DROP_STOP_SESSION",
    "DROP_ITEM_SAFE",
    "WOODCUT_START_DROP_SESSION",
    "WOODCUT_STOP_DROP_SESSION",
    "WOODCUT_DROP_ITEM_SAFE",
    "EAT_FOOD_SAFE",
    "CLOSE_BANK",
    "LOGOUT_SAFE",
    "STOP_ALL_RUNTIME",
    "LOGIN_START_TEST",
};

std::string normalize_command_type(std::string command_type) {
  std::transform(command_type.begin(), command_type.end(), command_type.begin(), [](unsigned char c) {
    return static_cast<char>(std::toupper(c));
  });
  return command_type;
}

} // namespace

bool is_supported_command_type(const std::string& command_type) {
  const std::string normalized = normalize_command_type(command_type);
  return std::any_of(kSupportedCommandTypes.begin(), kSupportedCommandTypes.end(), [&](const char* candidate) {
    return normalized == candidate;
  });
}

} // namespace xptool::bridge
