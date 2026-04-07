import statistics
import unittest

from runelite_planner.runtime_core.motion_engine import MotionEngine


class MotionEngineTests(unittest.TestCase):
    def test_woodcutting_cadence_is_slower_and_more_variable_than_default(self) -> None:
        engine = MotionEngine(seed=42)
        woodcut_pre: list[int] = []
        woodcut_post: list[int] = []
        default_pre: list[int] = []
        default_post: list[int] = []

        for _ in range(200):
            woodcut_payload = {"interactionKind": "CHOP_NEAREST_TREE"}
            engine.decorate_payload(woodcut_payload)
            woodcut_pre.append(int(woodcut_payload["preClickDelayMs"]))
            woodcut_post.append(int(woodcut_payload["postClickDelayMs"]))

            default_payload = {"interactionKind": "MINE_NEAREST_ROCK"}
            engine.decorate_payload(default_payload)
            default_pre.append(int(default_payload["preClickDelayMs"]))
            default_post.append(int(default_payload["postClickDelayMs"]))

        self.assertGreater(statistics.mean(woodcut_pre), statistics.mean(default_pre))
        self.assertGreater(statistics.mean(woodcut_post), statistics.mean(default_post))
        self.assertGreater((max(woodcut_pre) - min(woodcut_pre)), (max(default_pre) - min(default_pre)))
        self.assertGreater((max(woodcut_post) - min(woodcut_post)), (max(default_post) - min(default_post)))

    def test_woodcutting_branch_respects_payload_bounds(self) -> None:
        engine = MotionEngine(seed=7)
        for _ in range(300):
            payload = {"interactionKind": "CHOP_NEAREST_TREE"}
            engine.decorate_payload(payload)
            self.assertGreaterEqual(int(payload["preClickDelayMs"]), 4)
            self.assertLessEqual(int(payload["preClickDelayMs"]), 170)
            self.assertGreaterEqual(int(payload["postClickDelayMs"]), 4)
            self.assertLessEqual(int(payload["postClickDelayMs"]), 110)
            self.assertGreaterEqual(float(payload["mouseDriftRadius"]), 0.0)


if __name__ == "__main__":
    unittest.main()

