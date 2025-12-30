import functions_framework
import vertexai
from vertexai.preview.vision_models import ImageGenerationModel
import base64
import json

# Initialize Vertex AI - uses default service account credentials
vertexai.init(project="lobbylens-482801", location="us-central1")

@functions_framework.http
def generate_image(request):
    """HTTP Cloud Function to generate images using Vertex AI Imagen."""
    
    # Handle CORS preflight
    if request.method == 'OPTIONS':
        headers = {
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Methods': 'POST',
            'Access-Control-Allow-Headers': 'Content-Type',
            'Access-Control-Max-Age': '3600'
        }
        return ('', 204, headers)

    headers = {'Access-Control-Allow-Origin': '*'}

    try:
        request_json = request.get_json(silent=True)
        
        if not request_json or 'prompt' not in request_json:
            return (json.dumps({'error': 'Missing prompt in request body'}), 400, headers)

        prompt = request_json['prompt']
        
        # Load the Imagen model
        model = ImageGenerationModel.from_pretrained("imagegeneration@006")
        
        # Generate image
        images = model.generate_images(
            prompt=prompt,
            number_of_images=1,
            aspect_ratio="1:1",
            safety_filter_level="block_some",
            person_generation="allow_adult",
        )
        
        if not images:
            return (json.dumps({'error': 'No image generated'}), 500, headers)

        # Convert to base64
        image_bytes = images[0]._image_bytes
        image_base64 = base64.b64encode(image_bytes).decode('utf-8')
        
        return (json.dumps({'image': image_base64}), 200, headers)

    except Exception as e:
        return (json.dumps({'error': str(e)}), 500, headers)
