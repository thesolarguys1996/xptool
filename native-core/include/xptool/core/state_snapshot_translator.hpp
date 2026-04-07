#pragma once

namespace xptool::core {

class StateSnapshot;

class StateSnapshotTranslator {
public:
  void ensure_woodcutting_candidates(StateSnapshot* snapshot) const;
};

} // namespace xptool::core
