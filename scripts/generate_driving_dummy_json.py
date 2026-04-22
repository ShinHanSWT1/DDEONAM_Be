#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import random
from dataclasses import dataclass
from datetime import datetime, timedelta
from pathlib import Path


@dataclass(frozen=True)
class VehicleProfile:
    vehicle_size: str
    fuel_type: str


DRIVER_STYLES = ("ECO", "NORMAL", "AGGRESSIVE")


def choose_driver_style() -> str:
    return random.choices(DRIVER_STYLES, weights=[0.45, 0.45, 0.1], k=1)[0]


def normalize_fuel_type(fuel_type: str | None) -> str:
    if not fuel_type:
        return "GASOLINE"
    normalized = fuel_type.strip().upper()
    if normalized in {"DIESEL", "HYBRID"}:
        return normalized
    return "GASOLINE"


def normalize_body_type(body_type: str | None) -> str:
    if not body_type:
        return "MIDSIZE"
    normalized = body_type.strip().upper()
    if "SMALL" in normalized:
        return "SMALL"
    if "LARGE" in normalized or "SUV" in normalized:
        return "LARGE"
    return "MIDSIZE"


def resolve_vehicle_profile(style: str, fuel_type: str | None = None, body_type: str | None = None) -> VehicleProfile:
    if fuel_type or body_type:
        return VehicleProfile(
            normalize_body_type(body_type),
            normalize_fuel_type(fuel_type),
        )
    if style == "ECO":
        return VehicleProfile("MIDSIZE", random.choice(["HYBRID", "GASOLINE"]))
    if style == "AGGRESSIVE":
        return VehicleProfile(random.choice(["MIDSIZE", "LARGE"]), random.choice(["GASOLINE", "DIESEL"]))
    return VehicleProfile(random.choice(["SMALL", "MIDSIZE"]), random.choice(["GASOLINE", "HYBRID"]))


def generate_event_counts(style: str) -> tuple[int, int, int, int]:
    if style == "ECO":
        return random.randint(0, 1), random.randint(0, 1), 0, random.randint(1, 3)
    if style == "AGGRESSIVE":
        return random.randint(1, 3), random.randint(1, 3), random.randint(0, 2), random.randint(3, 7)
    return random.randint(0, 2), random.randint(0, 1), random.randint(0, 1), random.randint(2, 5)


def calculate_distance_factor(profile: VehicleProfile) -> float:
    size_factor = {"SMALL": 0.9, "MIDSIZE": 1.0, "LARGE": 1.12}[profile.vehicle_size]
    fuel_factor = {"GASOLINE": 1.0, "DIESEL": 1.08, "HYBRID": 0.95}[profile.fuel_type]
    return size_factor * fuel_factor


def calculate_idle_factor(profile: VehicleProfile) -> float:
    size_factor = {"SMALL": 0.9, "MIDSIZE": 1.0, "LARGE": 1.1}[profile.vehicle_size]
    fuel_factor = {"GASOLINE": 1.0, "DIESEL": 1.1, "HYBRID": 0.7}[profile.fuel_type]
    return size_factor * fuel_factor


def calculate_speed_bonus(profile: VehicleProfile) -> float:
    size_bonus = {"SMALL": 2.0, "MIDSIZE": 0.0, "LARGE": -2.0}[profile.vehicle_size]
    fuel_bonus = {"GASOLINE": 0.0, "DIESEL": 3.0, "HYBRID": -1.0}[profile.fuel_type]
    return size_bonus + fuel_bonus


def adjust_event_counts_for_profile(profile: VehicleProfile, rapid_accel_count: int, hard_brake_count: int,
                                    overspeed_count: int, fallback_idle: int) -> tuple[int, int, int, int]:
    if profile.vehicle_size == "LARGE":
        hard_brake_count += 1
        fallback_idle += 1
    elif profile.vehicle_size == "SMALL":
        rapid_accel_count = max(rapid_accel_count - 1, 0)

    if profile.fuel_type == "HYBRID":
        rapid_accel_count = max(rapid_accel_count - 1, 0)
        hard_brake_count = max(hard_brake_count - 1, 0)
        fallback_idle = max(fallback_idle - 1, 1)
    elif profile.fuel_type == "DIESEL":
        overspeed_count += 1 if random.random() < 0.3 else 0

    return rapid_accel_count, hard_brake_count, overspeed_count, fallback_idle


def calculate_safety_score(rapid_accel_count: int, hard_brake_count: int, overspeed_count: int) -> int:
    score = 100 - (rapid_accel_count * 4) - (hard_brake_count * 5) - (overspeed_count * 6)
    return max(0, min(100, round(score)))


def calculate_eco_score(idling_time_minutes: int, driving_time_minutes: int, rapid_accel_count: int, hard_brake_count: int) -> int:
    idle_ratio = idling_time_minutes / max(driving_time_minutes, 1)
    score = 100 - (idle_ratio * 50) - (rapid_accel_count * 5) - (hard_brake_count * 3)
    return max(0, min(100, round(score)))


def calculate_carbon_emission(distance_km: float, idling_time_minutes: int, rapid_accel_count: int, hard_brake_count: int,
                              profile: VehicleProfile) -> float:
    base_factor = {"GASOLINE": 150, "DIESEL": 130, "HYBRID": 90}[profile.fuel_type]
    size_factor = {"SMALL": 0.8, "MIDSIZE": 1.0, "LARGE": 1.2}[profile.vehicle_size]
    idle_factor = {"GASOLINE": 20, "DIESEL": 25, "HYBRID": 10}[profile.fuel_type]
    accel_factor = 8 if profile.fuel_type == "HYBRID" else {"SMALL": 10, "MIDSIZE": 15, "LARGE": 20}[profile.vehicle_size]
    decel_factor = {"SMALL": 7, "MIDSIZE": 10, "LARGE": 12}[profile.vehicle_size]

    co2_g = (
        distance_km * base_factor * size_factor
        + idling_time_minutes * idle_factor
        + rapid_accel_count * accel_factor
        + hard_brake_count * decel_factor
    )
    return round(co2_g / 1000, 4)


def calculate_carbon_reduction(distance_km: float, idling_time_minutes: int, rapid_accel_count: int, hard_brake_count: int,
                               profile: VehicleProfile, actual_emission_kg: float) -> float:
    size_factor = {"SMALL": 0.8, "MIDSIZE": 1.0, "LARGE": 1.2}[profile.vehicle_size]
    baseline_co2_g = (
        distance_km * 160 * size_factor
        + idling_time_minutes * 25
        + rapid_accel_count * 18
        + hard_brake_count * 12
    )
    reduction_kg = max((baseline_co2_g / 1000) - actual_emission_kg, 0)
    return round(reduction_kg, 4)


def calculate_reward_point(carbon_reduction_kg: float) -> int:
    return int(carbon_reduction_kg * 100)


def build_events(started_at: datetime, rapid_accel_count: int, hard_brake_count: int, overspeed_count: int) -> list[dict]:
    events: list[dict] = []
    offsets = []
    total_count = rapid_accel_count + hard_brake_count + overspeed_count
    for _ in range(total_count):
        offsets.append(random.randint(3, 30))
    offsets.sort()

    event_types = (
        ["RAPID_ACCEL"] * rapid_accel_count
        + ["HARD_BRAKE"] * hard_brake_count
        + ["OVERSPEED"] * overspeed_count
    )
    random.shuffle(event_types)

    for idx, event_type in enumerate(event_types):
        score_delta = {"RAPID_ACCEL": -4, "HARD_BRAKE": -5, "OVERSPEED": -6}[event_type]
        events.append({
            "event_type": event_type,
            "event_value": 1,
            "occurred_at": (started_at + timedelta(minutes=offsets[idx])).isoformat(timespec="seconds"),
            "score_delta": score_delta,
        })
    return events


def build_session_payload(user_id: int, user_vehicle_id: int, run_at: datetime,
                          fuel_type: str | None = None, body_type: str | None = None) -> dict:
    style = choose_driver_style()
    profile = resolve_vehicle_profile(style, fuel_type, body_type)
    driving_time_minutes = random.randint(20, 70)
    base_idle_minutes = random.randint(1, 8)
    distance_km = round(random.uniform(6, 35) * calculate_distance_factor(profile), 2)
    started_at = run_at.replace(minute=0, second=0, microsecond=0) - timedelta(hours=random.randint(1, 8))
    started_at += timedelta(minutes=random.randint(0, 50))
    rapid_accel_count, hard_brake_count, overspeed_count, fallback_idle = generate_event_counts(style)
    rapid_accel_count, hard_brake_count, overspeed_count, fallback_idle = adjust_event_counts_for_profile(
        profile, rapid_accel_count, hard_brake_count, overspeed_count, fallback_idle
    )
    idling_time_minutes = max(round(base_idle_minutes * calculate_idle_factor(profile)), fallback_idle)
    average_speed = round(max((distance_km / (driving_time_minutes / 60)) + calculate_speed_bonus(profile), 8.0), 2)
    max_speed = round(max(average_speed + random.uniform(15, 35), average_speed), 2)
    ended_at = started_at + timedelta(minutes=driving_time_minutes)

    safety_score = calculate_safety_score(rapid_accel_count, hard_brake_count, overspeed_count)
    eco_score = calculate_eco_score(idling_time_minutes, driving_time_minutes, rapid_accel_count, hard_brake_count)
    carbon_emission_kg = calculate_carbon_emission(
        distance_km, idling_time_minutes, rapid_accel_count, hard_brake_count, profile
    )
    carbon_reduction_kg = calculate_carbon_reduction(
        distance_km, idling_time_minutes, rapid_accel_count, hard_brake_count, profile, carbon_emission_kg
    )
    reward_point = calculate_reward_point(carbon_reduction_kg)

    return {
        "external_key": f"user-{user_id}-{started_at.strftime('%Y%m%d%H%M%S')}",
        "user_id": user_id,
        "user_vehicle_id": user_vehicle_id,
        "session_date": started_at.date().isoformat(),
        "started_at": started_at.isoformat(timespec="seconds"),
        "ended_at": ended_at.isoformat(timespec="seconds"),
        "distance_km": distance_km,
        "driving_time_minutes": driving_time_minutes,
        "idling_time_minutes": idling_time_minutes,
        "average_speed": average_speed,
        "max_speed": max_speed,
        "driver_style": style,
        "vehicle_size": profile.vehicle_size,
        "fuel_type": profile.fuel_type,
        "rapid_accel_count": rapid_accel_count,
        "hard_brake_count": hard_brake_count,
        "overspeed_count": overspeed_count,
        "safety_score": safety_score,
        "eco_score": eco_score,
        "carbon_emission_kg": carbon_emission_kg,
        "carbon_reduction_kg": carbon_reduction_kg,
        "reward_point": reward_point,
        "events": build_events(started_at, rapid_accel_count, hard_brake_count, overspeed_count),
    }


def write_batch(output_dir: Path, sessions: list[dict], run_at: datetime, user_id: int) -> Path:
    batch_id = f"batch-{run_at.strftime('%Y%m%d-%H%M%S')}-user-{user_id}"
    output_dir.mkdir(parents=True, exist_ok=True)
    tmp_path = output_dir / f"{batch_id}.json.tmp"
    final_path = output_dir / f"{batch_id}.json"
    payload = {
        "batch_id": batch_id,
        "generated_at": run_at.isoformat(timespec="seconds"),
        "sessions": sessions,
    }
    tmp_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    tmp_path.rename(final_path)
    return final_path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="주행 더미데이터 JSON 배치 생성기")
    parser.add_argument("--user-id", type=int, default=1)
    parser.add_argument("--user-vehicle-id", type=int, default=1)
    parser.add_argument("--seed", type=int)
    parser.add_argument("--run-at", type=str)
    parser.add_argument("--fuel-type", type=str)
    parser.add_argument("--body-type", type=str)
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=Path(__file__).resolve().parents[1] / "generated" / "driving" / "pending",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    if args.seed is not None:
        random.seed(args.seed)
    run_at = datetime.fromisoformat(args.run_at) if args.run_at else datetime.now()
    sessions = [
        build_session_payload(
            args.user_id,
            args.user_vehicle_id,
            run_at,
            args.fuel_type,
            args.body_type,
        )
    ]
    batch_path = write_batch(args.output_dir, sessions, run_at, args.user_id)
    print(f"generated: {batch_path}")


if __name__ == "__main__":
    main()
