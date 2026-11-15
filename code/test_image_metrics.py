import base64
import io
import json
import unittest
from unittest.mock import patch
from PIL import Image
from image_metrics import normalize_metrics, recognize_image
from llm_provider import ModelError, ModelSettings, generate_text


class ImageMetricsTest(unittest.TestCase):
    def test_only_missing_duration_is_derived_and_marked(self):
        result = normalize_metrics({'distanceKm': 5.2, 'paceSecondsPerKm': 369})
        self.assertAlmostEqual(31.98, result['durationMin'])
        self.assertTrue(result['durationEstimated'])
        self.assertTrue(result['warnings'])

    def test_mismatch_warns_and_preserves_readings(self):
        result = normalize_metrics({'distanceKm': 5, 'durationSeconds': 1800, 'paceSecondsPerKm': 500})
        self.assertEqual(30, result['durationMin'])
        self.assertEqual(500, result['paceSecondsPerKm'])
        self.assertIn('不一致', result['warnings'][0])
        subtle = normalize_metrics({'distanceKm': 5.2, 'durationSeconds': 1980, 'paceSecondsPerKm': 369})
        self.assertIn('不一致', subtle['warnings'][0])

    def test_unknown_and_invalid_values_are_not_invented(self):
        for value in [None, True, '5km', -1, 0, float('nan'), float('inf'), 1e10]:
            result = normalize_metrics(dict(distanceKm=value, durationSeconds=value, paceSecondsPerKm=value))
            self.assertIsNone(result['distanceKm'])
            self.assertIsNone(result['durationMin'])
            self.assertIsNone(result['paceSecondsPerKm'])

    @patch('image_metrics.generate_text')
    def test_invalid_image_never_calls_model(self, call):
        for data in ['invalid', base64.b64encode(b'not an image').decode()]:
            with self.assertRaises(ModelError): recognize_image(data)
        call.assert_not_called()

    @patch('image_metrics.ModelSettings.from_env')
    @patch('image_metrics.generate_text')
    def test_decodes_reencodes_and_validates_model_result(self, call, settings):
        image = io.BytesIO()
        Image.new('RGB', (40, 40), 'white').save(image, format='PNG')
        settings.return_value = ModelSettings('compatible', 'https://example.test', 'vision', 'test')
        call.return_value = json.dumps({'distanceKm': 5.2, 'durationSeconds': 1920, 'paceSecondsPerKm': 369})
        result = recognize_image(base64.b64encode(image.getvalue()).decode())
        self.assertEqual(32, result['durationMin'])
        self.assertTrue(call.call_args.kwargs['image_data'].startswith('data:image/jpeg;base64,'))
        self.assertEqual(400, call.call_args.args[1])
        for value in ['not json', '[]']:
            call.return_value = value
            with self.assertRaises(ModelError): recognize_image(base64.b64encode(image.getvalue()).decode())

    @patch('llm_provider.urlopen')
    def test_multimodal_payload_does_not_change_text_requests(self, call):
        response = call.return_value.__enter__.return_value
        response.read.return_value = json.dumps({'choices': [{'finish_reason': 'stop', 'message': {'content': '{}'}}]}).encode()
        config = ModelSettings('compatible', 'https://example.test', 'vision', 'test')
        generate_text('read screenshot', 400, True, config, 'data:image/jpeg;base64,dGVzdA==')
        body = json.loads(call.call_args.args[0].data)
        self.assertEqual('image_url', body['messages'][0]['content'][1]['type'])
        self.assertEqual({'type': 'json_object'}, body['response_format'])
        generate_text('plain', 400, settings=config)
        self.assertEqual('plain', json.loads(call.call_args.args[0].data)['messages'][0]['content'])


if __name__ == '__main__': unittest.main()
