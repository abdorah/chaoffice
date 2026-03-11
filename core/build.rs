use std::io::Result;

fn main() -> Result<()> {
    // Compile protobuf files with prost (uses system protoc)
    let out_dir = std::path::PathBuf::from("src/models");
    std::fs::create_dir_all(&out_dir).ok();

    prost_build::Config::new()
        .out_dir(&out_dir)
        .compile_protos(
            &[
                "../proto/sweetlab/models.proto",
                "../proto/sweetlab/auth.proto",
                "../proto/sweetlab/services.proto",
            ],
            &["../proto"],
        )?;

    // UniFFI scaffolding
    uniffi::generate_scaffolding("src/sweet_lab_core.udl")
        .expect("UniFFI scaffolding generation failed");

    Ok(())
}
