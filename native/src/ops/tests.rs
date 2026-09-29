//! Drift gate for the operation inventory.
//!
//! `native/operations.txt` is the shared contract between the Rust dispatcher and the Kotlin
//! bridge: `OperationParityTest` in the Kotlin module asserts the same file equals the set of
//! `@Operation` names there.

use std::collections::{BTreeMap, BTreeSet};
use std::fs;
use std::path::PathBuf;

use super::{all_operations, route, INVENTORY};

/// Operations that JNI exports implement directly instead of routing through [`super::dispatch`].
const JNI_OPERATIONS: &[&str] = &["invokeRaw", "isAuthorized", "signInBot"];

fn inventory_file() -> PathBuf {
    PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("operations.txt")
}

fn lib_source() -> String {
    fs::read_to_string(PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("src/lib.rs"))
        .expect("src/lib.rs must be readable")
}

fn inventory_lines() -> Vec<String> {
    let path = inventory_file();
    fs::read_to_string(&path)
        .unwrap_or_else(|error| panic!("{} must be readable: {error}", path.display()))
        .lines()
        .map(str::to_owned)
        .collect()
}

#[test]
fn every_declared_operation_routes() {
    for (index, operations) in INVENTORY.iter().enumerate() {
        for operation in operations.iter() {
            assert!(
                route(operation).is_some(),
                "{operation} is declared by module {index} but no module routes it"
            );
        }
    }
}

#[test]
fn operation_names_are_unique_across_modules() {
    let mut owners: BTreeMap<&str, usize> = BTreeMap::new();
    for (index, operations) in INVENTORY.iter().enumerate() {
        for operation in operations.iter() {
            if let Some(other) = owners.insert(operation, index) {
                panic!("{operation} is declared by modules {other} and {index}");
            }
        }
    }
}

#[test]
fn inventory_file_matches_the_dispatcher() {
    let mut expected = all_operations();
    expected.extend_from_slice(JNI_OPERATIONS);
    expected.sort_unstable();
    let expected: BTreeSet<String> = expected.into_iter().map(str::to_owned).collect();

    let actual: BTreeSet<String> = inventory_lines().into_iter().collect();
    let missing: Vec<&String> = expected.difference(&actual).collect();
    let extra: Vec<&String> = actual.difference(&expected).collect();
    assert!(
        missing.is_empty() && extra.is_empty(),
        "native/operations.txt is out of date; missing: {missing:?}, extra: {extra:?}"
    );
}

#[test]
fn inventory_file_is_sorted_and_free_of_duplicates() {
    let lines = inventory_lines();
    let mut sorted = lines.clone();
    sorted.sort();
    assert_eq!(lines, sorted, "native/operations.txt must be sorted");
    assert_eq!(
        lines.len(),
        lines.iter().collect::<BTreeSet<_>>().len(),
        "native/operations.txt must not repeat an operation"
    );
}

#[test]
fn jni_operations_are_exported() {
    let source = lib_source();
    for operation in JNI_OPERATIONS {
        assert!(
            source.contains(&format!(
                "fn Java_org_kotlogramme_TelegramClient_00024Native_{operation}("
            )),
            "{operation} has no matching #[no_mangle] JNI export"
        );
    }
    assert_eq!(
        source.matches("#[no_mangle]").count(),
        7,
        "src/lib.rs must keep exactly seven JNI exports"
    );
}
