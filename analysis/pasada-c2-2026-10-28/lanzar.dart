// Lanza una pasada «Volver a corregir» dentro de la C2 como la lanza el tablero, para que se vea en Ejecuciones.
// Uso: dart --packages=<vocabulary_dashboard>/.dart_tool/package_config.json lanzar.dart <etiqueta> <tope>
//      ... lanzar.dart <etiqueta> conciliar
import 'dart:convert';
import 'dart:io';
import 'package:vocabulary_dashboard/services/content_audit_cli_service.dart';
import 'package:vocabulary_dashboard/services/corrida_desprendida.dart';
import 'package:vocabulary_dashboard/tablero_html/ejecuciones/perfiles_de_corrida.dart';

Future<void> main(List<String> args) async {
  const root = '/Users/josecullen/projects/learney/content-audit';
  final folder = File(Platform.script.toFilePath()).parent.path;
  final etiqueta = args[0];
  final environment = <String, String>{
    ...Platform.environment,
    'CONTENT_AUDIT_ROOT': root,
    'CONTENT_AUDIT_WORKDIR': '$root/.content-audit/campaigns/a1-v11',
    'CONTENT_AUDIT_CONTEXT_KIND': 'Propuesta',
    'CONTENT_AUDIT_CONTEXT_LABEL': 'A1 · V11',
  };
  final cli = ContentAuditCliService.fromEnvironment(environment);
  final record = File('$folder/lanzada-$etiqueta.json');
  if (args.length > 1 && args[1] == 'conciliar') {
    final saved = jsonDecode(await record.readAsString()) as Map;
    final done = await cli.finishDetachedRun(saved['runId'] as String);
    stdout.writeln(jsonEncode({'conciliada': done, 'runId': saved['runId']}));
    return;
  }
  if (await record.exists()) {
    stdout.writeln('Ya se lanzó; no lanzo otra corrida paga.');
    stdout.writeln(await record.readAsString());
    return;
  }
  final tope = int.parse(args[1]);
  final preflight = (jsonDecode(await File('$folder/estimacion-$etiqueta.json').readAsString()) as Map)
      .cast<String, dynamic>();
  final profiles = PerfilesDeCorrida.desdeDisco(root, environment);
  final settings = await profiles.resolver(PedidoDePerfil.desdeJson({'id': 'codex-chatgpt', 'esfuerzo': 'medium'}));
  if (settings.model != 'gpt-6.1-sol') throw StateError('Cambió el modelo; no lanzo');
  final mark = await cli.startQuizCorrectionDetached(
    expectedBatchId: preflight['batchId'] as String,
    queryCap: tope,
    preflight: preflight,
    settings: settings,
    origin: OrigenDeLanzamiento(perfil: 'codex-chatgpt', pedidoId: 'pasada-c2-$etiqueta-20261028'),
    parentEnvironment: environment,
  );
  await record.writeAsString('${const JsonEncoder.withIndent('  ').convert(mark.aJson())}\n');
  stdout.writeln(jsonEncode({'runId': mark.runId, 'batchId': preflight['batchId'], 'tope': tope,
    'modelo': settings.model, 'esfuerzo': settings.reasoningEffort}));
}
