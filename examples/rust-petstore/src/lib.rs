#[rustfmt::skip]
pub mod generated;
pub mod serialization;
pub mod server;
pub mod transportation;

use std::collections::HashMap;

/// Raw requests and responses keep every query parameter and header as a name with all its values.
fn group(pairs: impl IntoIterator<Item = (String, String)>) -> HashMap<String, Vec<String>> {
    pairs
        .into_iter()
        .fold(HashMap::new(), |mut map, (name, value)| {
            map.entry(name).or_default().push(value);
            map
        })
}

fn ungroup(map: &HashMap<String, Vec<String>>) -> impl Iterator<Item = (&String, &String)> {
    map.iter()
        .flat_map(|(name, values)| values.iter().map(move |value| (name, value)))
}
