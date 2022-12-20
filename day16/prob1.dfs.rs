use bitvec::prelude::*;
use regex::Regex;
use std::collections::HashMap;
use std::{cmp, env, io};
// use typed_arena::Arena;

#[derive(Debug)]
struct Valve {
    id: i32,
    rate: i32,
    edges: Vec<i32>,
}

#[derive(Debug)]
enum Action {
    MoveTo(i32),
    Open(i32),
}

#[derive(Eq, Hash, PartialEq)]
struct SearchState {
    opened: BitVec,
    current: i32,
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
        .map(|valve_tuple| {
            let valve = Valve {
                id: *valves_to_id.get(&valve_tuple.0).unwrap(),
                rate: valve_tuple.1,
                edges: valve_tuple
                    .2
                    .iter()
                    .map(|name| *valves_to_id.get(name).unwrap())
                    .collect(),
            };
            (valve.id, valve)
        })
        .collect();

    let start_valve = graph.get(valves_to_id.get("AA").unwrap()).unwrap();
    let mut search_cache = HashMap::new();
    let mut opened = BitVec::repeat(false, graph.len());
    let (max_pressure, mut path) = search_path(
        &graph,
        start_valve,
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
    current_valve: &Valve,
    opened_valves: &mut BitVec,
    remaining_minutes: i32,
    pressure_per_sec: i32,
    pressure_released: i32,
    search_cache: &mut HashMap<SearchState, i32>,
) -> (i32, Vec<Action>) {
    if remaining_minutes == 0 {
        return (pressure_released, vec![]);
    }
    let (max_if_not_open_current, path_if_not_open) = visit_next(
        graph,
        current_valve,
        opened_valves,
        remaining_minutes,
        pressure_per_sec,
        pressure_released,
        search_cache,
    );
    if current_valve.rate > 0 && !*opened_valves.get(current_valve.id as usize).unwrap() {
        opened_valves.set(current_valve.id as usize, true);
        let (max_if_open_current, mut path_if_open_current) = visit_next(
            graph,
            current_valve,
            opened_valves,
            remaining_minutes - 1,
            pressure_per_sec + current_valve.rate,
            pressure_released + pressure_per_sec,
            search_cache,
        );
        path_if_open_current.push(Action::Open(current_valve.id));
        opened_valves.set(current_valve.id as usize, false);

        return cmp::max_by_key(
            (max_if_not_open_current, path_if_not_open),
            (max_if_open_current, path_if_open_current),
            |pair| pair.0,
        );
    };
    return (max_if_not_open_current, path_if_not_open);
}

fn visit_next(
    graph: &HashMap<i32, Valve>,
    current_valve: &Valve,
    opened_valves: &mut BitVec,
    remaining_minutes: i32,
    pressure_per_sec: i32,
    pressure_released: i32,
    search_cache: &mut HashMap<SearchState, i32>,
) -> (i32, Vec<Action>) {
    if remaining_minutes == 0 {
        return (pressure_released, vec![]);
    }
    let curr_state = SearchState {
        opened: opened_valves.clone(),
        current: current_valve.id,
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

    let res = current_valve
        .edges
        .iter()
        .map(|next| {
            let next_valve = graph.get(next).unwrap();
            let (pressure, mut path) = search_path(
                graph,
                next_valve,
                opened_valves,
                remaining_minutes - 1,
                pressure_per_sec,
                pressure_released + pressure_per_sec,
                search_cache,
            );
            path.push(Action::MoveTo(next_valve.id));
            return (pressure, path);
        })
        .max_by_key(|pair| pair.0)
        .unwrap();
    return res;
}
