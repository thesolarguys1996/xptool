#include "xptool/core/state_snapshot.hpp"

namespace xptool::core {

bool StateSnapshot::is_valid() const { return tick >= 0; }

} // namespace xptool::core
