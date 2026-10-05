import Alert from '@mui/material/Alert'
import Container from '@mui/material/Container'
import Typography from '@mui/material/Typography'
import { useQuery } from '@tanstack/react-query'

async function fetchHealth(): Promise<{ status: string }> {
  const response = await fetch('/api/health')
  if (!response.ok) {
    throw new Error(`Health check failed with status ${response.status}`)
  }
  return response.json()
}

function App() {
  const health = useQuery({ queryKey: ['health'], queryFn: fetchHealth })

  return (
    <Container maxWidth="sm" sx={{ mt: 8 }}>
      <Typography variant="h4" component="h1" gutterBottom>
        SedzKitchens
      </Typography>
      {health.isPending && <Alert severity="info">Checking backend…</Alert>}
      {health.isError && (
        <Alert severity="error">Backend is not reachable</Alert>
      )}
      {health.isSuccess && (
        <Alert severity="success">Backend status: {health.data.status}</Alert>
      )}
    </Container>
  )
}

export default App
