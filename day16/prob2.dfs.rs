use bitvec::prelude::*;
use itertools::{iproduct, Itertools};
use regex::Regex;
use std::collections::{BinaryHeap, HashMap, HashSet};
use std::{cmp, env, io};
// use typed_arena::Arena;

#[derive(Debug)]
struct Valve {
    id: i32,
    rate: i32,
    edges: Vec<i32>,
}

#[derive(Debug, Clone, PartialEq)]
enum Action {
    MoveTo(i32),
    Open(i32),
}

#[derive(Eq, Hash, PartialEq)]
struct SearchState {
    opened: BitVec,
    current: BitVec, // Or-ed of two positions
    remaining_minutes: i32,
}

fn main() {
    // let arena = Arena::new();

    let args: Vec<String> = env::args().collect();
    let minutes = args
        .get(1)
        .and_then(|arg| arg.parse::<i32>().ok())
        .unwrap_or(30);
    println!("Using minutes: {}", minutes);

    let re =
        Regex::new(r"Valve (\S+) has flow rate=(\d+); tunnels? leads? to valves? (.*)").unwrap();

    let valves: Vec<_> = io::stdin()
        .lines()
        .map(|line| {
            let l = line.unwrap();
            let cap = re.captures(l.as_str()).unwrap();
            let name = cap[1].to_string();
            let rate = cap[2].parse::<i32>().unwrap();
            let leads_to: Vec<_> = cap[3].split(", ").map(|s| s.to_owned()).collect();

            // let valve = Valve::new(name.clone(), rate, leads_to);
            println!("Valve: {} rate: {} leads to {:?}", name, rate, &leads_to);
            (name, rate, leads_to)
        })
        .collect();

    let valves_to_id: HashMap<String, i32> = valves.iter().map(|v| v.0.clone()).zip(0..).collect();
    // let id_to_valves: HashMap<_, _> = valves_to_id.iter().map(|(k, v)| (*v, k)).collect();

    let graph: HashMap<_, _> = valves
        .into_iter()
        .map(|valve| {
            let valve_id = Valve {
                id: *valves_to_id.get(&valve.0).unwrap(),
                rate: valve.1,
                edges: valve
                    .2
                    .iter()
                    .map(|name| *valves_to_id.get(name).unwrap())
                    .collect(),
            };
            (valve_id.id, valve_id)
        })
        .collect();

    let start_valve = valves_to_id["AA"];
    let mut search_cache = HashMap::new();
    let mut opened = BitVec::repeat(false, graph.len());
    let (max_pressure, mut path) = search_path(
        &graph,
        (start_valve, start_valve),
        &mut opened,
        minutes,
        0,
        0,
        &mut search_cache,
    );
    path.reverse();
    println!("Max releasable pressure {}, path {:?}", max_pressure, path);
}

fn search_path(
    graph: &HashMap<i32, Valve>,
    current_valves: (i32, i32),
    opened_valves: &mut BitVec,
    remaining_minutes: i32,
    pressure_per_sec: i32,
    pressure_released: i32,
    search_cache: &mut HashMap<SearchState, i32>,
) -> (i32, Vec<(Action, Action)>) {
    if remaining_minutes == 0 {
        return (pressure_released, vec![]);
    }

    let mut current_valves_mask = BitVec::repeat(false, graph.len());
    current_valves_mask.set(current_valves.0 as usize, true);
    current_valves_mask.set(current_valves.1 as usize, true);
    let curr_state = SearchState {
        opened: opened_valves.clone(),
        current: current_valves_mask,
        remaining_minutes: remaining_minutes,
    };
    let current_score = pressure_per_sec * remaining_minutes + pressure_released;
    if let Some(cached_highest_score) = search_cache.get(&curr_state) {
        if current_score <= *cached_highest_score {
            // prune!
            return (pressure_released, vec![]);
        }
    };
    search_cache.insert(curr_state, current_score);

    let valve1 = graph.get(&current_valves.0).unwrap();
    let valve2 = graph.get(&current_valves.1).unwrap();
    let actions1 = create_action_candidates(valve1, opened_valves);
    let actions2 = create_action_candidates(valve2, opened_valves);

    let max = iproduct!(actions1.into_iter(), actions2.into_iter())
        .filter(|(a1, a2)| {
            // apparently we shouldn't try to open a same valve twice
            if let Action::Open(_to_open) = a1 {
                return a1 != a2;
            }
            true
        })
        .map(|(a1, a2)| {
            let (next_rate, next_valve1) = match a1 {
                Action::Open(to_open) => {
                    opened_valves.set(to_open as usize, true);
                    (
                        pressure_per_sec + graph.get(&to_open).unwrap().rate,
                        to_open,
                    )
                }
                Action::MoveTo(move_to) => (pressure_per_sec, move_to),
            };

            let (next_rate_2, next_valve2) = match a2 {
                Action::Open(to_open) => {
                    opened_valves.set(to_open as usize, true);
                    (next_rate + graph.get(&to_open).unwrap().rate, to_open)
                }
                Action::MoveTo(move_to) => (next_rate, move_to),
            };

            let (pressure, mut path) = search_path(
                graph,
                (next_valve1, next_valve2),
                opened_valves,
                remaining_minutes - 1,
                next_rate_2,
                pressure_released + pressure_per_sec,
                search_cache,
            );
            if let Action::Open(open1) = a1 {
                opened_valves.set(open1 as usize, false);
            }
            if let Action::Open(open2) = a2 {
                opened_valves.set(open2 as usize, false);
            }
            path.push((a1, a2));

            (pressure, path)
        })
        .max_by_key(|pair| pair.0)
        .unwrap();

    return max;
}

fn create_action_candidates(current_valve: &Valve, opened_valves: &BitVec) -> Vec<Action> {
    let mut actions = Vec::with_capacity(current_valve.edges.len() + 1);
    if current_valve.rate > 0 && !*opened_valves.get(current_valve.id as usize).unwrap() {
        actions.push(Action::Open(current_valve.id));
    }
    for next in current_valve.edges.iter() {
        actions.push(Action::MoveTo(*next));
    }
    actions
}
