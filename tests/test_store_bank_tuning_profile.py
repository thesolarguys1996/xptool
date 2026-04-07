import argparse
import unittest

from runelite_planner.store_bank import (
    MODE_THESSALIA_SKIRT_BUYER,
    STORE_BANK_TUNING_PROFILE_DB_PARITY,
    StoreBankConfig,
    StoreBankStrategy,
    add_args,
    build_strategy,
)


class StoreBankTuningProfileTests(unittest.TestCase):
    def test_store_bank_uses_db_parity_profile(self) -> None:
        strategy = StoreBankStrategy(
            cfg=StoreBankConfig(
                mode=MODE_THESSALIA_SKIRT_BUYER,
                tuning_profile=STORE_BANK_TUNING_PROFILE_DB_PARITY,
            )
        )
        delegate = strategy._delegate
        self.assertEqual(STORE_BANK_TUNING_PROFILE_DB_PARITY, delegate._tuning.profile_key)

    def test_store_bank_normalizes_legacy_profile_tokens_to_db_parity(self) -> None:
        strategy = StoreBankStrategy(
            cfg=StoreBankConfig(
                mode=MODE_THESSALIA_SKIRT_BUYER,
                tuning_profile="TUNING",
            )
        )
        delegate = strategy._delegate
        self.assertEqual(STORE_BANK_TUNING_PROFILE_DB_PARITY, delegate._tuning.profile_key)

    def test_cli_builder_accepts_store_bank_db_parity_profile(self) -> None:
        parser = argparse.ArgumentParser()
        add_args(parser)
        args = parser.parse_args(
            [
                "--store-bank-mode",
                MODE_THESSALIA_SKIRT_BUYER,
                "--store-bank-tuning-profile",
                STORE_BANK_TUNING_PROFILE_DB_PARITY,
            ]
        )
        strategy = build_strategy(args)
        delegate = strategy._delegate
        self.assertEqual(STORE_BANK_TUNING_PROFILE_DB_PARITY, delegate._tuning.profile_key)

    def test_cli_builder_rejects_legacy_profile_tokens(self) -> None:
        parser = argparse.ArgumentParser()
        add_args(parser)
        with self.assertRaises(SystemExit):
            parser.parse_args(
                [
                    "--store-bank-mode",
                    MODE_THESSALIA_SKIRT_BUYER,
                    "--store-bank-tuning-profile",
                    "TUNING",
                ]
            )


if __name__ == "__main__":
    unittest.main()
